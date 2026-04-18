package cc.riskswap.trader.base.event;

import lombok.Data;

@Data
public class TaskLogEvent {
    private String appName;
    private String taskType;
    private String taskCode;
    private String taskName;
    private String traceId;
    private String instanceId;
    private Long costMs;
    private String status;
    private String remark;
}