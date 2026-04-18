package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChartDataDto {
    private String label; // Can be date or category
    private BigDecimal value;
}
