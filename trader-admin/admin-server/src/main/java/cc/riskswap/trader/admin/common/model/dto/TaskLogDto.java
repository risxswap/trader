package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskLogDto {
    private Long id;
    private String taskName;
    private String taskGroup;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status;
    private String content;
    private String errorMsg;
    private Long executionMs;
    private OffsetDateTime createdAt;
}