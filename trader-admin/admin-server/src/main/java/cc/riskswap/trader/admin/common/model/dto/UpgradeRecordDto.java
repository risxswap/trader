package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class UpgradeRecordDto {
    private Integer id;
    private String version;
    private String title;
    private String description;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String operator;
    private String errorMessage;
    private String checksum;
}
