package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 投资Dto
 */
@Data
public class InvestmentDto {
    private Integer id;
    private String name;
    private String groupName;
    private String targetType;
    private String investType;
    private Integer brokerId;
    private List<String> targets;
    private BigDecimal budget;
    private StrategyInfoDto strategyInfo;
    private String cron;
    private String executorId;
    private String status;
    private BigDecimal profitAmount;
    private BigDecimal profitRate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime updatedAt;
}
