package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SystemTaskDto {
    private Long id;
    private String appName;
    private String taskType;
    private String sourceType;
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
