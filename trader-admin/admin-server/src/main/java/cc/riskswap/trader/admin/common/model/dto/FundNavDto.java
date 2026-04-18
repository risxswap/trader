package cc.riskswap.trader.admin.common.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class FundNavDto {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime time;
    
    private String code;
    
    private BigDecimal unitNav;
    
    private BigDecimal accumNav;
    
    private BigDecimal accumDiv;
    
    private BigDecimal netAsset;
    
    private BigDecimal totalNetAsset;
    
    private BigDecimal adjNav;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime updatedAt;
}
