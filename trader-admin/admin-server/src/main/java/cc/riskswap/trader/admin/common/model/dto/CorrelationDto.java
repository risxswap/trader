package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 证券相关性Dto
 */
@Data
public class CorrelationDto {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String asset1;
    private String asset1Type;
    private String asset2;
    private String asset2Type;
    private BigDecimal coefficient;
    private BigDecimal pValue;
    private String period;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private OffsetDateTime updatedAt;
}
