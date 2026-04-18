package cc.riskswap.trader.executor.strategy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.riskswap.trader.base.task.StrategyTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.ObjectUtils;
import java.util.Set;

import cc.riskswap.trader.executor.common.ExecutorContext;
import cc.riskswap.trader.executor.common.enums.TradingStatusEnum;
import cc.riskswap.trader.executor.common.model.dto.PositionStateDto;
import cc.riskswap.trader.executor.common.model.param.TradingParam;
import cc.riskswap.trader.executor.dao.InvestmentDao;
import cc.riskswap.trader.executor.dao.InvestmentLogDao;
import cc.riskswap.trader.executor.dao.InvestmentPositionDao;
import cc.riskswap.trader.executor.dao.InvestmentTradingDao;
import cc.riskswap.trader.executor.dao.entity.Investment;
import cc.riskswap.trader.executor.dao.entity.InvestmentLog;
import cc.riskswap.trader.executor.dao.entity.InvestmentPosition;
import cc.riskswap.trader.executor.dao.entity.InvestmentTrading;
import cc.riskswap.trader.executor.pubsub.publisher.InvestmentLogPublisher;
import cc.riskswap.trader.base.strategy.config.BaseStrategyConfig;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseStrategy<T extends BaseStrategyConfig> implements StrategyTask {

    /**
     * 策略名称
     */
    protected static String name;

    protected static String desc;
    

    protected T config;

    /**
     * 策略上下文
     */
    protected final ExecutorContext context = new ExecutorContext();

    @Autowired
    private InvestmentDao investmentDao;

    @Autowired
    private InvestmentPositionDao investmentPositionDao;

    @Autowired
    private InvestmentTradingDao investmentTradingDao;

    @Autowired
    private InvestmentLogDao investmentLogDao;

    @Autowired
    private InvestmentLogPublisher investmentLogPublisher;

    @Override
    public void execute(TraderTaskContext context) {
        // Query active investments that use this strategy
        List<Investment> activeInvestments = investmentDao.listByStatus(Set.of("RUNNING"));
        
        for (Investment investment : activeInvestments) {
            if (investment == null || investment.getId() == null) {
                continue;
            }
            // Check if this investment's strategy matches this class
            if (!this.getClass().getName().equals(investment.getStrategy())) {
                continue;
            }
            
            try {
                initContext(investment.getId());
                run(this.context);
            } catch (Exception e) {
                log.error("Failed to execute strategy for investment {}", investment.getId(), e);
            }
        }
    }

    /**
     * 初始化上下文
     */
    private void initContext(Long investmentId) {
        Investment investment = investmentDao.getById(investmentId);
        this.context.setInvestment(investment);
        // 设置策略参数
        String configStr = investment.getStrategyConfig();
        if (!ObjectUtils.isEmpty(configStr)) {
            @SuppressWarnings("unchecked")
            Class<T> configClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), BaseStrategy.class);
            if (configClass != null) {
                this.config = JSONUtil.parseObj(configStr).toBean(configClass);
            }
        }
        // 查询最新的日志
        InvestmentLog latestLog = investmentLogDao.getLatestByInvestmentId(investmentId);
        this.context.setLatestLog(latestLog);
        // 查询最新的持仓
        Map<String, InvestmentPosition> positions = new HashMap<>();
        List<InvestmentPosition> positionList = investmentPositionDao.getListByInvestmentId(investmentId);
        for (InvestmentPosition position : positionList) {
            positions.put(this.context.getAssetKey(position.getAsset(), position.getAssetType()), position);
        }
        this.context.setPositions(positions);

        // 查询最新的交易
        Map<String, InvestmentTrading> traddings = new HashMap<>();
        List<InvestmentTrading> tradingList = investmentTradingDao.getListByInvestmentId(investmentId);
        for (InvestmentTrading trading : tradingList) {
            String key = this.context.getAssetKey(trading.getAsset(), trading.getAssetType());
            traddings.putIfAbsent(key, trading);
        }
        this.context.setTraddings(traddings);
    }

    /**
     * 运行策略
     */
    public abstract void run(ExecutorContext context);

    /**
     * 交易
     */
    public void trade(List<TradingParam> traddings) {
        if (traddings == null || traddings.isEmpty()) {
            return;
        }

        Investment investment = getValidInvestment();
        if (investment == null) {
            return;
        }

        Long investmentId = investment.getId();
        BigDecimal budget = investment.getBudget() != null ? investment.getBudget() : BigDecimal.ZERO;
        BigDecimal baseCash = getBaseCash(investmentId, budget);

        TradeData tradeData = prepareTradeData(traddings, investmentId, baseCash);

        InvestmentLog logEntity = saveInitialLog(investmentId, tradeData.cash);
        if (logEntity == null) {
            return;
        }

        Map<String, PositionStateDto> finalStates = processTrades(traddings, investmentId, logEntity.getId(), tradeData.initialStates);

        // 记录交易日志
        finalizeLog(logEntity, finalStates, tradeData.lastPriceMap, budget);
        
        // 发布日志广播
        investmentLogPublisher.create(logEntity.getId());
    }

    private Investment getValidInvestment() {
        Investment investment = this.context.getInvestment();
        if (investment == null || investment.getId() == null) {
            log.warn("Investment is null, skip trade: {}", name);
            return null;
        }
        return investment;
    }

    private BigDecimal getBaseCash(Long investmentId, BigDecimal budget) {
        InvestmentLog latestLog = investmentLogDao.getLatestByInvestmentId(investmentId);
        if (latestLog != null && latestLog.getCash() != null) {
            return latestLog.getCash();
        }
        return budget;
    }

    private TradeData prepareTradeData(List<TradingParam> traddings, Long investmentId, BigDecimal baseCash) {
        BigDecimal cash = baseCash;
        Map<String, PositionStateDto> initialStates = new HashMap<>();
        Map<String, BigDecimal> lastPriceMap = new HashMap<>();

        for (TradingParam param : traddings) {
            if (!isValid(param)) {
                continue;
            }
            String type = getParamType(param);
            BigDecimal price = param.getPrice();
            String key = getParamKey(param);

            if (!initialStates.containsKey(key)) {
                InvestmentPosition pos = this.context.getPositions().get(key);
                initialStates.put(key, PositionStateDto.from(pos));
            }

            BigDecimal volume;
            if (param.isSaleAll()) {
                PositionStateDto currentState = initialStates.get(key);
                if (currentState != null && currentState.getNetQuantity().signum() != 0) {
                    volume = currentState.getNetQuantity().negate();
                } else {
                    volume = BigDecimal.ZERO;
                }
                param.setVolume(volume);
            } else {
                volume = param.getVolume();
            }

            cash = cash.add(volume.multiply(price).negate());
            lastPriceMap.put(key, price);
        }
        return new TradeData(cash, initialStates, lastPriceMap);
    }

    private InvestmentLog saveInitialLog(Long investmentId, BigDecimal cash) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        InvestmentLog logEntity = new InvestmentLog();
        logEntity.setInvestmentId(investmentId);
        logEntity.setRecordDate(now);
        logEntity.setCash(cash);
        logEntity.setCreatedAt(now);
        logEntity.setUpdatedAt(now);

        if (!investmentLogDao.save(logEntity)) {
            log.error("Failed to save investment log, investmentId={}", investmentId);
            return null;
        }
        return logEntity;
    }

    private Map<String, PositionStateDto> processTrades(List<TradingParam> traddings, Long investmentId, Long investmentLogId, Map<String, PositionStateDto> initialStates) {
        Map<String, PositionStateDto> states = new HashMap<>(initialStates.size());
        List<InvestmentTrading> tradingList = new ArrayList<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (TradingParam param : traddings) {
            if (!isValid(param)) {
                continue;
            }
            
            String key = getParamKey(param);
            PositionStateDto state = initialStates.getOrDefault(key, new PositionStateDto(param.getSymbol(), getParamType(param)));
            state.processTrade(param);
            states.put(key, state);

            tradingList.add(createTradingRecord(param, investmentId, investmentLogId, now));
        }

        if (!tradingList.isEmpty()) {
            investmentTradingDao.saveBatch(tradingList);
        }
        return states;
    }

    private InvestmentTrading createTradingRecord(TradingParam param, Long investmentId, Long investmentLogId, java.time.LocalDateTime now) {
        InvestmentTrading trading = new InvestmentTrading();
        trading.setInvestmentId(investmentId);
        trading.setInvestmentLogId(investmentLogId);
        trading.setAsset(param.getSymbol());
        trading.setAssetType(getParamType(param));
        trading.setVolume(param.getVolume());
        trading.setPrice(param.getPrice());
        trading.setStatus(TradingStatusEnum.PENDING.name());
        trading.setCreatedAt(now);
        trading.setUpdatedAt(now);
        return trading;
    }

    private void finalizeLog(InvestmentLog logEntity, Map<String, PositionStateDto> finalStates, Map<String, BigDecimal> lastPriceMap, BigDecimal budget) {
        BigDecimal asset = BigDecimal.ZERO;
        for (Map.Entry<String, PositionStateDto> entry : finalStates.entrySet()) {
            BigDecimal lastPrice = lastPriceMap.get(entry.getKey());
            if (lastPrice == null) {
                continue;
            }
            PositionStateDto state = entry.getValue();
            if (state == null || state.getNetQuantity().signum() == 0) {
                continue;
            }
            asset = asset.add(state.getNetQuantity().multiply(lastPrice));
        }
        logEntity.setAsset(asset);
        // Note: Profit logic might be moved elsewhere or we can just ignore setting it since entity has no profit field
        logEntity.setUpdatedAt(java.time.LocalDateTime.now());
        investmentLogDao.updateById(logEntity);
    }

    private boolean isValid(TradingParam param) {
        if (param == null || param.getPrice() == null) {
            return false;
        }
        if (!param.isSaleAll() && param.getVolume() == null) {
            return false;
        }
        return param.getSymbol() != null && !param.getSymbol().isBlank();
    }

    private String getParamType(TradingParam param) {
        return param.getType() == null ? "" : param.getType();
    }

    private String getParamKey(TradingParam param) {
        return this.context.getAssetKey(param.getSymbol(), getParamType(param));
    }

    private static class TradeData {
        final BigDecimal cash;
        final Map<String, PositionStateDto> initialStates;
        final Map<String, BigDecimal> lastPriceMap;

        TradeData(BigDecimal cash, Map<String, PositionStateDto> initialStates, Map<String, BigDecimal> lastPriceMap) {
            this.cash = cash;
            this.initialStates = initialStates;
            this.lastPriceMap = lastPriceMap;
        }
    }
}
