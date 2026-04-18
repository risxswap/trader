package cc.riskswap.trader.admin.common.model.query;

import lombok.Data;

@Data
public class SystemTaskListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String appName;
    private String taskType;
    private Boolean includeInvestment;
    private String taskCode;
    private String taskName;
    private String status;
}
