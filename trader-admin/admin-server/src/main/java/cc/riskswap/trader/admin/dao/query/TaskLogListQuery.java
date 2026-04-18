package cc.riskswap.trader.admin.dao.query;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskLogListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String taskCode;
    private String taskName;
    private String status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
