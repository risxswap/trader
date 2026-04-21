package cc.riskswap.trader.statistic.task;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import cc.riskswap.trader.base.task.StatisticTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.statistic.service.CorrelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CorrelationTask implements StatisticTask {

    @Autowired
    private FundDao fundDao;

    @Autowired
    private CorrelationService correlationService;

    @Override
    public String getTaskCode() {
        return "statistic.correlation.calc";
    }

    @Override
    public String getTaskName() {
        return "资产相关性计算任务";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String getParamSchema() {
        return "{}";
    }

    @Override
    public String getDefaultParams() {
        return "{}";
    }

    @Override
    @TraderTaskLog("资产相关性计算任务")
    public void execute(TraderTaskContext context) {
        doCalculateAllCorrelations();
    }

    public void doCalculateAllCorrelations() {
        log.info("Start calculating all fund correlations...");
        long start = System.currentTimeMillis();

        List<Fund> funds = fundDao.listAll();
        if (funds == null || funds.isEmpty()) {
            log.info("No funds found to calculate.");
            return;
        }

        List<Fund> uniqueFunds = funds.stream()
                .filter(f -> f.getCode() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Fund::getCode))),
                        ArrayList::new
                ));
        int count = correlationService.calculateAndSaveBatch(uniqueFunds, "1Y");
        int deletedCount = correlationService.cleanupHistoricalCorrelations();
        long elapsed = System.currentTimeMillis() - start;
        log.info("Calculated {} fund correlations for {} funds and cleaned {} historical records. Time elapsed: {} ms",
                count, uniqueFunds.size(), deletedCount, elapsed);
    }
}
