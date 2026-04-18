package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.InvestmentDto;
import cc.riskswap.trader.admin.common.model.param.InvestmentParam;
import cc.riskswap.trader.admin.common.model.query.InvestmentQuery;
import cc.riskswap.trader.base.dao.InvestmentLogDao;
import cc.riskswap.trader.base.dao.InvestmentDao;
import cc.riskswap.trader.base.dao.entity.Investment;
import cc.riskswap.trader.base.dao.entity.InvestmentLog;
import cc.riskswap.trader.admin.pubsub.publisher.InvestmentPublisher;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.exception.Warning;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投资服务
 */
@Slf4j
@Service
public class InvestmentService {

    @Autowired
    private InvestmentDao investmentDao;

    @Autowired
    private InvestmentPublisher investmentPublisher;

    @Autowired
    private cc.riskswap.trader.admin.cache.StrategyInfoCache strategyInfoCache;

    @Autowired
    private InvestmentLogDao investmentLogDao;

    private InvestmentDto toDto(Investment investment) {
        if (investment == null) {
            return null;
        }
        InvestmentDto dto = BeanUtil.copyProperties(investment, InvestmentDto.class);
        
        // 组装策略信息
        if (StrUtil.isNotBlank(investment.getStrategy())) {
            cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto info = strategyInfoCache.getStrategy(investment.getStrategy());
            if (info == null) {
                // 如果缓存中没有，创建一个仅包含类名的基础对象
                info = new cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto();
                info.setClassName(investment.getStrategy());
                info.setName(investment.getStrategy());
            } else {
                // 必须使用副本，避免污染缓存对象
                cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto copy = new cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto();
                BeanUtil.copyProperties(info, copy);
                info = copy;
            }
            // 填充具体的配置值
            info.setConfig(investment.getStrategyConfig());
            dto.setStrategyInfo(info);
        }
        
        return dto;
    }

    public PageDto<InvestmentDto> list(InvestmentQuery query) {
        cc.riskswap.trader.base.dao.query.InvestmentListQuery listQuery = new cc.riskswap.trader.base.dao.query.InvestmentListQuery();
        BeanUtil.copyProperties(query, listQuery);
        Page<Investment> page = investmentDao.pageQuery(listQuery);

        PageDto<InvestmentDto> result = new PageDto<>();
        result.setTotal(page.getTotal());
        result.setPageNo((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        
        List<InvestmentDto> items = page.getRecords().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        fillLatestProfit(items);
        result.setItems(items);
        
        return result;
    }

    private void fillLatestProfit(List<InvestmentDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Integer> investmentIds = items.stream()
                .map(InvestmentDto::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (investmentIds.isEmpty()) {
            return;
        }

        List<InvestmentLog> logs = investmentLogDao.listByInvestmentIds(investmentIds);

        Map<Integer, InvestmentLog> latestLogMap = new HashMap<>();
        for (InvestmentLog log : logs) {
            if (log.getInvestmentId() == null) {
                continue;
            }
            if (!latestLogMap.containsKey(log.getInvestmentId())) {
                latestLogMap.put(log.getInvestmentId(), log);
            }
        }

        for (InvestmentDto item : items) {
            InvestmentLog latest = latestLogMap.get(item.getId());
            if (latest == null) {
                continue;
            }
            item.setProfitAmount(latest.getProfit());
            item.setProfitRate(calcProfitRate(latest.getProfit(), item.getBudget()));
        }
    }

    private BigDecimal calcProfitRate(BigDecimal profit, BigDecimal budget) {
        if (profit == null || budget == null) {
            return null;
        }
        if (budget.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return profit.divide(budget, 6, RoundingMode.HALF_UP);
    }

    public void add(InvestmentParam param) {
        Investment investment = BeanUtil.copyProperties(param, Investment.class);
        if (investment.getBrokerId() == null) {
            throw new IllegalArgumentException("brokerId不能为空");
        }
        validateStrategyConfig(investment.getStrategy(), investment.getStrategyConfig());
        // Default status
        if (StrUtil.isBlank(investment.getStatus())) {
            investment.setStatus("STOPPED");
        }
        if (StrUtil.isBlank(investment.getCron())) {
            investment.setCron("");
        }
        if (StrUtil.isBlank(investment.getExecutorId())) {
            investment.setExecutorId("");
        }
        investmentDao.save(investment);
        investmentPublisher.create(investment.getId());
    }

    public void update(InvestmentParam param) {
        if (param.getId() == null) {
            throw new IllegalArgumentException("ID cannot be null for update");
        }
        Investment investment = BeanUtil.copyProperties(param, Investment.class);
        validateStrategyConfig(investment.getStrategy(), investment.getStrategyConfig());
        Investment existing = investmentDao.getById(param.getId());
        if (investment.getBrokerId() == null && existing != null) {
            investment.setBrokerId(existing.getBrokerId());
        }
        if (StrUtil.isBlank(investment.getCron())) {
            investment.setCron(existing != null ? existing.getCron() : "");
        }
        if (StrUtil.isBlank(investment.getExecutorId())) {
            investment.setExecutorId(existing != null ? existing.getExecutorId() : "");
        }
        investmentDao.updateById(investment);
        investmentPublisher.update(investment.getId());
    }
    
    public void delete(Integer id) {
        Investment investment = investmentDao.getById(id);
        if (investment != null) {
            investmentDao.removeById(id);
            investmentPublisher.delete(investment.getId());
        }
    }

    public InvestmentDto get(Integer id) {
        Investment investment = investmentDao.getById(id);
        return toDto(investment);
    }

    private void validateStrategyConfig(String strategy, String strategyConfig) {
        if (StrUtil.isBlank(strategy)) {
            throw new Warning(ErrorCode.PARAM_INVALID.code(), "strategy不能为空");
        }
        cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto info = strategyInfoCache.getStrategy(strategy);
        if (info == null) {
            throw new Warning(ErrorCode.PARAM_INVALID.code(), "策略不存在");
        }
        if (StrUtil.isBlank(info.getConfigSchame())) {
            return;
        }
        if (StrUtil.isBlank(strategyConfig)) {
            throw new Warning(ErrorCode.PARAM_INVALID.code(), "strategyConfig不能为空");
        }
        JSONObject schema;
        JSONObject cfg;
        try {
            schema = JSONUtil.parseObj(info.getConfigSchame());
            cfg = JSONUtil.parseObj(strategyConfig);
        } catch (Exception e) {
            throw new Warning(ErrorCode.PARAM_INVALID.code(), "strategyConfig格式错误");
        }
        JSONArray required = schema.getJSONArray("required");
        if (required != null) {
            for (Object r : required) {
                String key = String.valueOf(r);
                if (!cfg.containsKey(key)) {
                    throw new Warning(ErrorCode.PARAM_INVALID.code(), "缺少必填字段: " + key);
                }
            }
        }
        JSONObject props = schema.getJSONObject("properties");
        if (props != null) {
            for (String key : props.keySet()) {
                JSONObject p = props.getJSONObject(key);
                if (p == null) continue;
                Object v = cfg.get(key);
                if (v == null) continue;
                String type = p.getStr("type");
                if (StrUtil.isNotBlank(type)) {
                    if ("integer".equalsIgnoreCase(type)) {
                        if (!(v instanceof Number) || Math.floor(((Number) v).doubleValue()) != ((Number) v).doubleValue()) {
                            throw new Warning(ErrorCode.PARAM_INVALID.code(), "字段" + key + "需为整数");
                        }
                    } else if ("number".equalsIgnoreCase(type)) {
                        if (!(v instanceof Number)) {
                            throw new Warning(ErrorCode.PARAM_INVALID.code(), "字段" + key + "需为数字");
                        }
                    } else if ("string".equalsIgnoreCase(type)) {
                        if (!(v instanceof CharSequence)) {
                            throw new Warning(ErrorCode.PARAM_INVALID.code(), "字段" + key + "需为字符串");
                        }
                    }
                }
                if (v instanceof Number) {
                    Number num = (Number) v;
                    if (p.containsKey("minimum")) {
                        double min = p.getDouble("minimum");
                        if (num.doubleValue() < min) {
                            throw new Warning(ErrorCode.PARAM_INVALID.code(), "字段" + key + "小于最小值" + min);
                        }
                    }
                    if (p.containsKey("maximum")) {
                        double max = p.getDouble("maximum");
                        if (num.doubleValue() > max) {
                            throw new Warning(ErrorCode.PARAM_INVALID.code(), "字段" + key + "大于最大值" + max);
                        }
                    }
                }
                JSONArray en = p.getJSONArray("enum");
                if (en != null && en.size() > 0) {
                    boolean ok = false;
                    for (Object e : en) {
                        if (String.valueOf(e).equals(String.valueOf(v))) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        throw new Warning(ErrorCode.PARAM_INVALID.code(), "字段" + key + "不在允许枚举范围");
                    }
                }
            }
        }
    }
}
