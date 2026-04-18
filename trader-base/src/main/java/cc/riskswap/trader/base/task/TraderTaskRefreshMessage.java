package cc.riskswap.trader.base.task;

import java.io.Serializable;

public record TraderTaskRefreshMessage(String taskType, String taskCode, Long version, String eventType) implements Serializable {
}
