package cc.riskswap.trader.base.event;

import lombok.Data;

@Data
public class SystemTaskStatusEvent {
    private String appName;
    private String taskType;
    private String taskCode;
    private String taskName;
    private String status;
    private String result;
    private String cron;
    private Boolean enabled;
    private Long version;
    private String paramsJson;
}
