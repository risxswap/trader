package cc.riskswap.trader.collector.service;

import com.google.common.collect.Lists;

import cc.riskswap.trader.collector.common.enums.FundMarketEnum;
import cc.riskswap.trader.collector.common.model.query.FundAdjQuery;
import cc.riskswap.trader.collector.common.util.DateUtil;
import cc.riskswap.trader.collector.common.util.TaskContentContext;
import cc.riskswap.trader.collector.repository.tushare.FundAdjTushare;
import cc.riskswap.trader.base.dao.FundAdjDao;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundAdj;
import cc.riskswap.trader.base.task.TraderTaskContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FundAdjService {
    @Autowired
    private FundAdjDao fundAdjDao;
    @Autowired
    private FundDao fundDao;
    @Autowired
    private FundAdjTushare fundAdjTushare;

    public void syncFundAdj(TraderTaskContext context) {
        log.info("开始同步基金复权因子");
        try {
            OffsetDateTime latestTradeDate = null;
            try {
                latestTradeDate = fundAdjDao.getLatestTradeDate();
            } catch (Exception e) {
                log.warn("获取最新交易日期失败", e);
                TaskContentContext.addError("获取最新复权日期失败: " + e.getMessage());
                context.report().addFailed(1);
                context.report().putErrorDetail("fundAdjLatestDateError", e.getMessage());
                return;
            }
            TaskContentContext.addAttribute("最近复权日期",
                    latestTradeDate == null ? "首次同步" : latestTradeDate.toLocalDate().toString());
            syncByTradeDate(context, latestTradeDate);
            log.info("基金复权因子同步完成");
        } catch (Exception e) {
            log.error("同步基金复权因子失败", e);
            TaskContentContext.addError("同步基金复权因子失败: " + e.getMessage());
            context.report().addFailed(1);
            context.report().putErrorDetail("fundAdjError", e.getMessage());
        }
    }

    public void syncByTradeDate(TraderTaskContext context, OffsetDateTime lastTradeDate) {
        log.info("按日期同步基金复权因子，最后交易日期: {}", lastTradeDate);
        if (lastTradeDate == null) {
            TaskContentContext.addDetail("基金复权同步", "未查到历史复权日期，跳过按日期同步");
            return;
        }
        LocalDate startDate = lastTradeDate.toLocalDate().plusDays(1);
        LocalDate endDate = LocalDate.now();
        TaskContentContext.addAttribute("复权同步区间", startDate + " ~ " + endDate);

        if (startDate.isAfter(endDate)) {
            log.info("数据已是最新，无需同步");
            TaskContentContext.addDetail("基金复权同步", "复权数据已是最新，无需同步");
            return;
        }

        LocalDate currentDate = startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        while (!currentDate.isAfter(endDate)) {
            String tradeDateStr = currentDate.format(formatter);
            log.info("同步日期: {}", tradeDateStr);

            FundAdjQuery query = new FundAdjQuery();
            query.setTradeDate(currentDate);

            List<FundAdj> fundAdjs = new ArrayList<>();
            Integer pageNo = 1;
            Integer pageSize = 2000;
            int pageCount = 0;
            while (true) {
                query.setPageNo(pageNo);
                query.setPageSize(pageSize);
                List<FundAdj> pageAdjs = fundAdjTushare.list(query);
                if (CollectionUtils.isEmpty(pageAdjs)) {
                    break;
                }
                pageCount++;
                fundAdjs.addAll(pageAdjs);
                if (pageAdjs.size() < pageSize) {
                    break;
                }
                pageNo++;
            }

            if (!CollectionUtils.isEmpty(fundAdjs)) {
                fundAdjDao.deleteByTime(DateUtil.toOffsetDateTime(currentDate));
                List<List<FundAdj>> partitions = Lists.partition(fundAdjs, 500);
                for (List<FundAdj> partition : partitions) {
                    fundAdjDao.saveBatch(partition);
                }
                context.report().addSynced(fundAdjs.size());
                TaskContentContext.addMetric("复权同步天数", 1);
                TaskContentContext.addMetric("复权拉取记录数", fundAdjs.size());
                TaskContentContext.addMetric("复权批次数", partitions.size());
                TaskContentContext.addDetail("基金复权同步",
                        String.format("%s 记录=%d,分页=%d,批次=%d", tradeDateStr, fundAdjs.size(), pageCount, partitions.size()));
                log.info("日期 {} 同步了 {} 条记录", tradeDateStr, fundAdjs.size());
            } else {
                TaskContentContext.addMetric("复权无数据天数", 1);
                TaskContentContext.addDetail("基金复权同步", tradeDateStr + " 无数据");
                log.info("日期 {} 无数据", tradeDateStr);
            }
            currentDate = currentDate.plusDays(1);
        }
        log.info("按日期同步基金复权因子完成");
    }

    public void syncBySymbol(TraderTaskContext context) {
        log.info("按基金代码同步复权因子");
        List<Fund> fundList = fundDao.listByMarket(FundMarketEnum.ETF.code);
        log.info("获取到 {} 个基金", fundList.size());
        TaskContentContext.addAttribute("复权补齐基金数", String.valueOf(fundList.size()));

        for (Fund fund : fundList) {
            try {
                syncBySymbol(context, fund.getCode());
            } catch (Exception e) {
                log.error("同步基金 {} 复权因子失败", fund.getCode(), e);
                TaskContentContext.addError(String.format("基金 %s 复权同步失败: %s", fund.getCode(), e.getMessage()));
            }
        }
        log.info("按基金代码同步复权因子完成");
    }

    public void syncBySymbol(TraderTaskContext context, String tsCode) {
        log.info("开始同步基金 {} 的复权因子", tsCode);
        Integer pageNo = 1;
        Integer pageSize = 2000;
        List<FundAdj> fundAdjs = new ArrayList<>();

        while (true) {
            FundAdjQuery query = new FundAdjQuery();
            query.setCode(tsCode);
            query.setPageNo(pageNo);
            query.setPageSize(pageSize);
            List<FundAdj> pageAdjs = fundAdjTushare.list(query);
            if (CollectionUtils.isEmpty(pageAdjs)) {
                break;
            }
            fundAdjs.addAll(pageAdjs);
            if (pageAdjs.size() < pageSize) {
                break;
            }
            pageNo++;
        }

        if (CollectionUtils.isEmpty(fundAdjs)) {
            log.info("基金 {} 无复权因子数据", tsCode);
            TaskContentContext.addDetail("基金复权补齐", tsCode + " 无复权数据");
            return;
        }

        fundAdjDao.deleteByCode(tsCode);
        List<List<FundAdj>> partitions = Lists.partition(fundAdjs, 500);
        for (List<FundAdj> partition : partitions) {
            fundAdjDao.saveBatch(partition);
        }
        context.report().addSynced(fundAdjs.size());
        TaskContentContext.addMetric("复权补齐记录数", fundAdjs.size());
        TaskContentContext.addDetail("基金复权补齐",
                String.format("%s 记录=%d,批次=%d", tsCode, fundAdjs.size(), partitions.size()));
        log.info("基金 {} 同步了 {} 条记录", tsCode, fundAdjs.size());
    }
}
