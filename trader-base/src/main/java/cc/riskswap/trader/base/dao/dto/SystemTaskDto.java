package cc.riskswap.trader.base.dao.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SystemTaskDto {
    private Long id;
    private String appName;
    private String taskCode;
    private String taskName;
    private String cron;
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
