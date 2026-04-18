package cc.riskswap.trader.admin.dao.base.query;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SystemTaskRunLogListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String appName;
    private String taskCode;
    private String status;
    private String triggerType;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
