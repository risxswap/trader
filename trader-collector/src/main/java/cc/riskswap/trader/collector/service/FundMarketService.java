package cc.riskswap.trader.collector.service;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

import cc.riskswap.trader.collector.common.enums.FundMarketEnum;
import cc.riskswap.trader.collector.common.enums.TimeFrameEnum;
import cc.riskswap.trader.collector.common.model.query.FundMarketQuery;
import cc.riskswap.trader.collector.common.util.DateUtil;
import cc.riskswap.trader.collector.common.util.TaskContentContext;
import cc.riskswap.trader.collector.repository.tushare.FundMarketTushare;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.FundMarketDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundMarket;
import lombok.extern.slf4j.Slf4j;

/**
 * Fund market data service implementation class, responsible for fund-related business logic processing
 */
@Service
@Slf4j
public class FundMarketService {
    
    @Autowired
    private FundMarketDao fundMarketDao;
    
    @Autowired
    private FundDao fundDao;

    @Autowired
    private FundMarketTushare fundMarketTushare;
    
    /**
     * Synchronize fund market data
     * 1. If the latest trade date differs from current date by more than 6 months, sync by fund code
     * 2. If the difference is less than 6 months, sync by date
     */
    public void syncFundMarket() {
        log.info("Starting to synchronize fund market data");
        try {
            // Get the latest trade date from database
            OffsetDateTime latestTradeDate = null;
            try {
                latestTradeDate = fundMarketDao.getLatestTradeDate();
                log.info("Retrieved latest trade date: {}", latestTradeDate);
            } catch (Exception e) {
                log.warn("Failed to get latest trade date", e);
                TaskContentContext.addError("获取最新行情日期失败: " + e.getMessage());
                return;
            }
            TaskContentContext.addAttribute("最近行情日期",
                    latestTradeDate == null ? "首次同步" : latestTradeDate.toLocalDate().toString());
            syncByTradeDate(latestTradeDate);
            log.info("Fund market data synchronization completed");
        } catch (Exception e) {
            log.error("Failed to synchronize fund market data", e);
            TaskContentContext.addError("同步基金行情失败: " + e.getMessage());
        }
    }

    /**
     * Synchronize fund market data by date
     * @param lastTradeDate Last trade date
     */
    public void syncByTradeDate(OffsetDateTime lastTradeDate) {
        log.info("Starting to synchronize fund market data by date, last trade date: {}", lastTradeDate);
        if (lastTradeDate == null) {
            TaskContentContext.addDetail("基金行情同步", "未查到历史行情日期，跳过按日期同步");
            return;
        }
        
        LocalDate startDate = lastTradeDate.toLocalDate().plusDays(1);
        LocalDate endDate = LocalDate.now();
        TaskContentContext.addAttribute("行情同步区间", startDate + " ~ " + endDate);
        
        if (startDate.isAfter(endDate)) {
            log.info("Data is already up-to-date, no need to sync");
            TaskContentContext.addDetail("基金行情同步", "行情数据已是最新，无需同步");
            return;
        }
        
        LocalDate currentDate = startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        
        while (true) {
            if (currentDate.isAfter(endDate) || currentDate.isEqual(endDate)) {
                break;
            }
            String tradeDateStr = currentDate.format(formatter);
            log.info("Synchronizing date: {}", tradeDateStr);
            
            FundMarketQuery query = new FundMarketQuery();
            query.setTradeDate(currentDate);
            
            List<FundMarket> fundMarkets = new ArrayList<>();
            Integer pageNo =1;
            Integer pageSize = 2000;
            int pageCount = 0;
            while (true) {
                query.setPageNo(pageNo);
                query.setPageSize(pageSize);
                List<FundMarket> pageMarkets = fundMarketTushare.list(query);
                if (pageMarkets == null || pageMarkets.isEmpty()) {
                    break;
                }
                pageCount++;
                fundMarkets.addAll(pageMarkets);
                if (pageMarkets.size() < pageSize) {
                    break;
                }
                pageNo++;
            }
            
            if (fundMarkets != null && !fundMarkets.isEmpty()) {
                fundMarketDao.deleteByTime(DateUtil.toOffsetDateTime(currentDate), TimeFrameEnum.D1.code);
                List<List<FundMarket>> partitions = Lists.partition(fundMarkets, 500);
                for (List<FundMarket> partition : partitions) {
                    fundMarketDao.saveBatch(partition);
                }
                TaskContentContext.addMetric("行情同步天数", 1);
                TaskContentContext.addMetric("行情拉取记录数", fundMarkets.size());
                TaskContentContext.addMetric("行情批次数", partitions.size());
                TaskContentContext.addDetail("基金行情同步",
                        String.format("%s 记录=%d,分页=%d,批次=%d", tradeDateStr, fundMarkets.size(), pageCount, partitions.size()));
                log.info("Date {} synchronized {} records", tradeDateStr, fundMarkets.size());
            } else {
                TaskContentContext.addMetric("行情无数据天数", 1);
                TaskContentContext.addDetail("基金行情同步", tradeDateStr + " 无数据");
                log.info("Date {} has no data", tradeDateStr);
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("Fund market data synchronization by date completed");
    }

    /**
     * Synchronize market data by fund code
     */
    public void syncBySymbol() {
        log.info("Starting to synchronize market data by fund code");
        
        List<Fund> fundList = fundDao.listByMarket(FundMarketEnum.ETF.code);
        log.info("Retrieved {} funds", fundList.size());
        TaskContentContext.addAttribute("行情补齐基金数", String.valueOf(fundList.size()));
        
        for (Fund fund : fundList) {
            try {
                String tsCode = fund.getCode();
                syncBySymbol(tsCode);
            } catch (Exception e) {
                log.error("Failed to synchronize market data for fund {}", fund.getCode(), e);
                TaskContentContext.addError(String.format("基金 %s 行情同步失败: %s", fund.getCode(), e.getMessage()));
            }
        }
        
        log.info("Market data synchronization by fund code completed");
    }

    public void syncBySymbol(String tsCode) {
        log.info("Starting to synchronize market data for fund {}", tsCode);
        
        Integer pageNo = 1;
        Integer pageSize = 2000;
        List<FundMarket> fundMarkets = new ArrayList<>();
        while (true) {
            FundMarketQuery query = new FundMarketQuery();
            query.setCode(tsCode);
            query.setPageNo(pageNo);
            query.setPageSize(pageSize);
            List<FundMarket> pageMarkets = fundMarketTushare.list(query);
            if (pageMarkets == null || pageMarkets.isEmpty()) {
                break;
            }
            fundMarkets.addAll(pageMarkets);
            if (pageMarkets.size() < pageSize) {
                break;
            }
            pageNo++;
        } 

        if (CollectionUtils.isEmpty(fundMarkets)) {
            log.info("Fund {} has no market data", tsCode);
            TaskContentContext.addDetail("基金行情补齐", tsCode + " 无行情数据");
            return;
        }
        fundMarketDao.deleteByCode(tsCode, TimeFrameEnum.D1.code);
        List<List<FundMarket>> partitions = Lists.partition(fundMarkets, 500);
        for (List<FundMarket> partition : partitions) {
            fundMarketDao.saveBatch(partition);
        }
        TaskContentContext.addMetric("行情补齐记录数", fundMarkets.size());
        TaskContentContext.addDetail("基金行情补齐",
                String.format("%s 记录=%d,批次=%d", tsCode, fundMarkets.size(), partitions.size()));
        log.info("Fund {} synchronized {} records", tsCode, fundMarkets.size());
    }

    public List<FundMarket> getData(String code, LocalDate beginDate, LocalDate endDate) {
        return fundMarketDao.getDailyData(code, beginDate, endDate);
    }
}
