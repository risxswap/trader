package cc.riskswap.trader.admin.dao.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SystemTaskRunLogDto {
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
