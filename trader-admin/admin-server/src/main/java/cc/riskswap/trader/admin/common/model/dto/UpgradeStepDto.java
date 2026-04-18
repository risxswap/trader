package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class UpgradeStepDto {
    private Integer id;
    private Integer orderNo;
    private String type;
    private String sqlText;
    private String status;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
}
