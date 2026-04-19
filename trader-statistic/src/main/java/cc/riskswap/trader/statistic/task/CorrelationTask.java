package cc.riskswap.trader.statistic.task;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import cc.riskswap.trader.base.task.StatisticTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.statistic.service.CorrelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CorrelationTask implements StatisticTask {

    @Autowired
    private FundDao fundDao;

    @Autowired
    private CorrelationService correlationService;

    @Autowired
    @Qualifier("correlationExecutor")
    private Executor correlationExecutor;

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

        Set<String> submittedCombinations = new HashSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int count = 0;
        int size = uniqueFunds.size();

        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                String asset1 = uniqueFunds.get(i).getCode();
                String asset2 = uniqueFunds.get(j).getCode();
                String key = asset1.compareTo(asset2) < 0 ? asset1 + "_" + asset2 : asset2 + "_" + asset1;
                if (!submittedCombinations.add(key)) {
                    continue;
                }

                futures.add(CompletableFuture.runAsync(
                        () -> correlationService.calculateAndSave(asset1, asset2, "1Y"),
                        correlationExecutor
                ));
                count++;
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        int deletedCount = correlationService.cleanupHistoricalCorrelations();
        long elapsed = System.currentTimeMillis() - start;
        log.info("Submitted {} correlation calculation tasks and cleaned {} historical records. Time elapsed: {} ms",
                count, deletedCount, elapsed);
    }
}
