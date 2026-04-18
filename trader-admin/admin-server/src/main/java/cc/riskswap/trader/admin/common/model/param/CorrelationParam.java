package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 证券相关性参数
 */
@Data
public class CorrelationParam {
    private Long id;
    private String asset1;
    private String asset1Type;
    private String asset2;
    private String asset2Type;
    private BigDecimal coefficient;
    private BigDecimal pValue;
    private String period;
}
