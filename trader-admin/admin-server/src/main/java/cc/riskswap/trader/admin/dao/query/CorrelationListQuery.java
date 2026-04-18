package cc.riskswap.trader.admin.dao.query;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CorrelationListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String asset1;
    private String asset2;
    private String period;
    private BigDecimal minCoefficient;
    private BigDecimal maxCoefficient;
}
