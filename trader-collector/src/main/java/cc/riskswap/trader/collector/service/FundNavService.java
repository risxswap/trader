package cc.riskswap.trader.collector.service;

import com.google.common.collect.Lists;

import cc.riskswap.trader.collector.common.model.query.FundNavQuery;
import cc.riskswap.trader.collector.common.util.DateUtil;
import cc.riskswap.trader.collector.common.util.TaskContentContext;
import cc.riskswap.trader.collector.repository.tushare.FundNavTushare;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.FundNavDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundNav;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class FundNavService {

    @Autowired
    private FundNavDao fundNavDao;

    @Autowired
    private FundDao fundDao;

    @Autowired
    private FundNavTushare fundNavTushare;

    public void syncFundNav(TraderTaskContext context) {
        log.info("开始同步基金净值");
        try {
            OffsetDateTime latestNavDate = null;
            try {
                latestNavDate = fundNavDao.getLatestNavDate();
            } catch (Exception e) {
                log.warn("获取最新净值日期失败", e);
                TaskContentContext.addError("获取最新净值日期失败: " + e.getMessage());
                context.report().addFailed(1);
                context.report().putErrorDetail("fundNavLatestDateError", e.getMessage());
                return;
            }
            TaskContentContext.addAttribute("最近净值日期", latestNavDate == null ? "首次同步" : latestNavDate.toLocalDate().toString());
            long synced = syncByNavDate(context, latestNavDate);
            context.report().putErrorDetail("fundNav", java.util.Map.of("synced", synced));
            log.info("基金净值同步完成");
        } catch (Exception e) {
            log.error("同步基金净值失败", e);
            TaskContentContext.addError("同步基金净值失败: " + e.getMessage());
            context.report().addFailed(1);
            context.report().putErrorDetail("fundNavError", e.getMessage());
        }
    }

    public long syncByNavDate(TraderTaskContext context, OffsetDateTime lastNavDate) {
        log.info("按日期同步基金净值，最后净值日期: {}", lastNavDate);
        if (lastNavDate == null) {
            TaskContentContext.addDetail("基金净值同步", "未查到历史净值日期，跳过按日期同步");
            return 0L;
        }
        LocalDate startDate = lastNavDate.toLocalDate().plusDays(1);
        LocalDate endDate = LocalDate.now();
        TaskContentContext.addAttribute("净值同步区间", startDate + " ~ " + endDate);

        if (startDate.isAfter(endDate)) {
            log.info("数据已是最新，无需同步");
            TaskContentContext.addDetail("基金净值同步", "净值数据已是最新，无需同步");
            return 0L;
        }

        long synced = 0L;
        LocalDate currentDate = startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        while (!currentDate.isAfter(endDate)) {
            String navDateStr = currentDate.format(formatter);
            log.info("同步日期: {}", navDateStr);
            List<FundNav> fundNavs = new ArrayList<>();
            Integer pageNo = 1;
            Integer pageSize = 5000;
            int pageCount = 0;
            while (true) {
                FundNavQuery query = new FundNavQuery();
                query.setNavDate(currentDate);
                query.setPageNo(pageNo);
                query.setPageSize(pageSize);
                List<FundNav> pageNavs = fundNavTushare.list(query);
                if (CollectionUtils.isEmpty(pageNavs)) {
                    break;
                }
                pageCount++;
                fundNavs.addAll(pageNavs);
                if (pageNavs.size() < pageSize) {
                    break;
                }
                pageNo++;
            }

            if (!CollectionUtils.isEmpty(fundNavs)) {
                // 根据 time 和 code 去重
                List<FundNav> distinctFundNavs = fundNavs.stream()
                        .filter(nav -> nav.getTime() != null && nav.getCode() != null)
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        nav -> nav.getTime().toString() + "#" + nav.getCode(),
                                        nav -> nav,
                                        (existing, replacement) -> existing
                                ),
                                map -> new ArrayList<>(map.values())
                        ));

                if (distinctFundNavs.size() < fundNavs.size()) {
                    log.info("日期 {} 原始数据 {} 条，去重后剩余 {} 条", navDateStr, fundNavs.size(), distinctFundNavs.size());
                }

                fundNavDao.deleteByTime(DateUtil.toOffsetDateTime(currentDate));
                List<List<FundNav>> partitions = Lists.partition(distinctFundNavs, 500);
                for (List<FundNav> partition : partitions) {
                    fundNavDao.saveBatch(partition);
                }
                context.report().addSynced(distinctFundNavs.size());
                synced += distinctFundNavs.size();
                TaskContentContext.addMetric("净值同步天数", 1);
                TaskContentContext.addMetric("净值拉取记录数", fundNavs.size());
                TaskContentContext.addMetric("净值入库记录数", distinctFundNavs.size());
                TaskContentContext.addMetric("净值批次数", partitions.size());
                TaskContentContext.addDetail("基金净值同步",
                        String.format("%s 原始=%d,去重后=%d,分页=%d,批次=%d",
                                navDateStr, fundNavs.size(), distinctFundNavs.size(), pageCount, partitions.size()));
                log.info("日期 {} 同步了 {} 条记录", navDateStr, distinctFundNavs.size());
            } else {
                TaskContentContext.addMetric("净值无数据天数", 1);
                TaskContentContext.addDetail("基金净值同步", navDateStr + " 无数据");
                log.info("日期 {} 无数据", navDateStr);
            }
            currentDate = currentDate.plusDays(1);
        }
        log.info("按日期同步基金净值完成");
        return synced;
    }

    public void syncBySymbol() {
        log.info("按基金代码同步净值");
        List<Fund> fundList = fundDao.listAll();
        log.info("获取到 {} 个基金", fundList.size());
        TaskContentContext.addAttribute("净值补齐基金数", String.valueOf(fundList.size()));

        for (Fund fund : fundList) {
            try {
                syncBySymbol(fund.getCode());
            } catch (Exception e) {
                log.error("同步基金 {} 净值失败", fund.getCode(), e);
                TaskContentContext.addError(String.format("基金 %s 净值同步失败: %s", fund.getCode(), e.getMessage()));
            }
        }
        log.info("按基金代码同步净值完成");
    }

    public void syncBySymbol(String tsCode) {
        log.info("开始同步基金 {} 的净值", tsCode);
        Integer pageNo = 1;
        Integer pageSize = 5000;
        List<FundNav> fundNavs = new ArrayList<>();

        while (true) {
            FundNavQuery query = new FundNavQuery();
            query.setCode(tsCode);
            query.setPageNo(pageNo);
            query.setPageSize(pageSize);
            List<FundNav> pageNavs = fundNavTushare.list(query);
            if (CollectionUtils.isEmpty(pageNavs)) {
                break;
            }
            fundNavs.addAll(pageNavs);
            if (pageNavs.size() < pageSize) {
                break;
            }
            pageNo++;
        }

        if (CollectionUtils.isEmpty(fundNavs)) {
            log.info("基金 {} 无净值数据", tsCode);
            TaskContentContext.addDetail("基金净值补齐", tsCode + " 无净值数据");
            return;
        }
        // 根据 time 和 code 去重
        List<FundNav> distinctFundNavs = fundNavs.stream()
                .filter(nav -> nav.getTime() != null && nav.getCode() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                nav -> nav.getTime().toString() + "#" + nav.getCode(),
                                nav -> nav,
                                (existing, replacement) -> existing
                        ),
                        map -> new ArrayList<>(map.values())
                ));
        log.info("基金 {} 原始数据 {} 条，去重后剩余 {} 条", tsCode, fundNavs.size(), distinctFundNavs.size());
        fundNavDao.deleteByCode(tsCode);
        List<List<FundNav>> partitions = Lists.partition(distinctFundNavs, 500);
        for (List<FundNav> partition : partitions) {
            fundNavDao.saveBatch(partition);
        }
        TaskContentContext.addMetric("净值补齐记录数", distinctFundNavs.size());
        TaskContentContext.addDetail("基金净值补齐",
                String.format("%s 原始=%d,去重后=%d,批次=%d", tsCode, fundNavs.size(), distinctFundNavs.size(), partitions.size()));
        log.info("基金 {} 同步了 {} 条记录", tsCode, distinctFundNavs.size());
    }
}
