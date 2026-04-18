package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class BrokerDto {
    private Integer id;
    private String name;
    private String code;
    private BigDecimal initialCapital;
    private BigDecimal currentCapital;
    private String intro;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private OffsetDateTime updatedAt;
}
