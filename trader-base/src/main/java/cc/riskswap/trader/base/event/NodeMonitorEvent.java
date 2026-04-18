package cc.riskswap.trader.base.event;

import lombok.Data;

@Data
public class NodeMonitorEvent {
    private String appName;
    private String nodeId;
    private String nodeType;
    private String status;
    private Long timestamp;
}