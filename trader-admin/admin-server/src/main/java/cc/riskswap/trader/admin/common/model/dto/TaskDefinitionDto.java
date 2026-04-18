package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class TaskDefinitionDto {
    private String taskType;
    private String taskCode;
    private String taskName;
    private String defaultCron;
    private Boolean defaultEnabled;
    private String paramSchema;
    private String defaultParamsJson;
    private String implClass;
    private String reportNodeId;
    private String reportNodeType;
    private String reportAt;
}
