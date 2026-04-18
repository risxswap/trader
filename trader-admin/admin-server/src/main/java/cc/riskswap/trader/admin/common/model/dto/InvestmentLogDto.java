package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 投资日志Dto
 */
@Data
public class InvestmentLogDto {
    private Integer id;
    private Integer investmentId;
    private OffsetDateTime recordDate;
    private String type;
    private BigDecimal cash;
    private BigDecimal asset;
    private BigDecimal profit;
    private String remark;
    private Integer notified;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
