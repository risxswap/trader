package cc.riskswap.trader.base.dao.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("system_task")
public class SystemTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appName;
    private String taskType;
    private String taskCode;
    private String taskName;
    private String cron;
    private Boolean enabled;
    private String status;
    private String result;
    private String paramSchema;
    private String paramsJson;
    private String defaultParamsJson;
    private Long version;
    private String remark;
    private OffsetDateTime updatedAt;
    private OffsetDateTime createdAt;
}
