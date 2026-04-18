package cc.riskswap.trader.admin.common.model.dto;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class FundAdjDto {
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private OffsetDateTime time;
    private String code;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private OffsetDateTime updatedAt;
    private Double adjFactor;
}
