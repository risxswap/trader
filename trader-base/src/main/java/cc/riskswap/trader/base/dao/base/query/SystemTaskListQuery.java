package cc.riskswap.trader.base.dao.base.query;

import lombok.Data;

@Data
public class SystemTaskListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String appName;
    private String taskType;
    private String taskCode;
    private String taskName;
    private String status;
}
