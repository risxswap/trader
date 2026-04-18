package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("task_log")
public class TaskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private String taskGroup;
    private String traceId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status;
    private String content;
    private String errorMsg;
    private Long executionMs;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
