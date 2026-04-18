package cc.riskswap.trader.base.task;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TraderTaskDefinition {
    private String taskType;
    private String taskCode;
    private String taskName;
    private Boolean defaultEnabled;
    private String paramSchema;
    private String defaultParamsJson;
    private String implClass;
    private String reportNodeId;
    private String reportNodeType;
    private OffsetDateTime reportAt;
}
