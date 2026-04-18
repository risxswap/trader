package cc.riskswap.trader.base.task;

import cn.hutool.core.util.StrUtil;

public enum TraderTaskType {
    COLLECTOR,
    STATISTIC,
    STRATEGY,
    ADMIN;

    public static TraderTaskType fromNodeType(String nodeType) {
        if (StrUtil.isBlank(nodeType)) {
            throw new IllegalArgumentException("nodeType is blank");
        }
        String normalized = nodeType.trim().toUpperCase();
        return TraderTaskType.valueOf(normalized);
    }
}