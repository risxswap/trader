package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class NodeStatusDto {
    private Long id;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Long nodeGroupId;
    private String nodeGroupName;
    private String approvalStatus;
    private String hostname;
    private String ipAddress;
    private String version;
    private Float cpuUsage;
    private Float memoryUsage;
    private Float diskUsage;
    private Long timestamp;
    private Boolean online;
    private String remark;
}
