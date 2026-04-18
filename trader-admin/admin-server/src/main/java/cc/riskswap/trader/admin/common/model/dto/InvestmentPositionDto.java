package cc.riskswap.trader.admin.common.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class InvestmentPositionDto {
    private Integer id;
    private Integer investmentId;
    private Integer investmentLogId;
    private String asset;
    private String assetType;
    private BigDecimal quantity;
    private BigDecimal buyPrice;
    private BigDecimal costPrice;
    private String side;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime updatedAt;
}
