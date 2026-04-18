package cc.riskswap.trader.admin.common.model.query;

import lombok.Data;

@Data
public class TaskDefinitionListQuery {
    private String taskType;
    private String taskCode;
    private String taskName;
}
