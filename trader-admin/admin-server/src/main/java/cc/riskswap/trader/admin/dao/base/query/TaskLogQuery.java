package cc.riskswap.trader.admin.dao.base.query;

import cc.riskswap.trader.admin.dao.base.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskLogQuery extends PageDto<Object> {
    private String taskName;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endTime;
}