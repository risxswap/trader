package cc.riskswap.trader.collector.common.util;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NumberUtil {

    /**
     * 解析数值为BigDecimal
     * @param value 数值对象
     * @return BigDecimal对象
     */
    public static BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return new BigDecimal(value.toString());
            } else if (value instanceof String) {
                return new BigDecimal((String) value);
            }
        } catch (Exception e) {
            log.error("解析数值失败: {}", value, e);
        }
        return null;
    }
}
