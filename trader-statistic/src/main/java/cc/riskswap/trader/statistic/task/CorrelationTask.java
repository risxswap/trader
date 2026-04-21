package cc.riskswap.trader.statistic.task;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
        JSONObject schema = new JSONObject();
        schema.set("type", "object");
        schema.set("title", "相关性过滤参数");
        schema.set("description", "配置相关系数结果过滤阈值");

        JSONObject properties = new JSONObject();
        properties.set("minAbsCorrelation", new JSONObject()
                .set("type", "number")
                .set("title", "最小绝对相关系数")
                .set("description", "仅保留绝对值大于该阈值的相关性结果")
                .set("default", CorrelationTaskParams.DEFAULT_MIN_ABS_CORRELATION)
                .set("minimum", 0)
                .set("maximum", 1));
        schema.set("properties", properties);
        return JSONUtil.toJsonStr(schema);
    }

    @Override
    public String getDefaultParams() {
        return JSONUtil.toJsonStr(new JSONObject()
                .set("minAbsCorrelation", CorrelationTaskParams.DEFAULT_MIN_ABS_CORRELATION));
    }

    @Override
    @TraderTaskLog("资产相关性计算任务")
    public void execute(TraderTaskContext context) {
        CorrelationTaskParams params = CorrelationTaskParams.fromJson(context == null ? null : context.getParamsJson());
        doCalculateAllCorrelations(params.minAbsCorrelation());
    }

    public void doCalculateAllCorrelations() {
        doCalculateAllCorrelations(CorrelationTaskParams.DEFAULT_MIN_ABS_CORRELATION);
    }

    public void doCalculateAllCorrelations(double minAbsCorrelation) {
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
        log.info("Loaded {} funds and reduced to {} unique funds for correlation calculation. minAbsCorrelation={}",
                funds.size(), uniqueFunds.size(), minAbsCorrelation);
        int count = correlationService.calculateAndSaveBatch(uniqueFunds, "1Y", minAbsCorrelation);
        log.info("Correlation calculation finished. saved={}, uniqueFunds={}, minAbsCorrelation={}",
                count, uniqueFunds.size(), minAbsCorrelation);
        int deletedCount = correlationService.cleanupHistoricalCorrelations();
        log.info("Historical correlation cleanup finished. deleted={}", deletedCount);
        long elapsed = System.currentTimeMillis() - start;
        log.info("Calculated {} fund correlations for {} funds and cleaned {} historical records. minAbsCorrelation={}, timeElapsedMs={}",
                count, uniqueFunds.size(), deletedCount, minAbsCorrelation, elapsed);
    }
}
