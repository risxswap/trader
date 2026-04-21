package cc.riskswap.trader.statistic.task;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record CorrelationTaskParams(double minAbsCorrelation) {

    public static final double DEFAULT_MIN_ABS_CORRELATION = 0.5d;

    public static CorrelationTaskParams fromJson(String json) {
        if (StrUtil.isBlank(json)) {
            return defaults();
        }

        try {
            JSONObject params = JSONUtil.parseObj(json);
            Double threshold = params.getDouble("minAbsCorrelation");
            if (threshold == null || Double.isNaN(threshold) || threshold < 0.0d || threshold > 1.0d) {
                log.warn("Invalid minAbsCorrelation in task params, fallback to default. paramsJson={}", json);
                return defaults();
            }
            return new CorrelationTaskParams(threshold);
        } catch (Exception e) {
            log.warn("Failed to parse correlation task params, fallback to default. paramsJson={}", json, e);
            return defaults();
        }
    }

    public static CorrelationTaskParams defaults() {
        return new CorrelationTaskParams(DEFAULT_MIN_ABS_CORRELATION);
    }
}
