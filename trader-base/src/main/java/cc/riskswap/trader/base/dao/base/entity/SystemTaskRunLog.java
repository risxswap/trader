package cc.riskswap.trader.base.dao.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("system_task_run_log")
public class SystemTaskRunLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appName;
    private String taskCode;
    private String triggerType;
    private String paramsJson;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Long durationMs;
    private String errorMsg;
    private String traceId;
    private OffsetDateTime createdAt;
}
