package cc.riskswap.trader.base.dao.param;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 投资日志参数
 */
@Data
public class InvestmentLogParam {
    private Integer id;
    private Integer investmentId;
    private OffsetDateTime recordDate;
    private String type;
    private BigDecimal cash;
    private BigDecimal asset;
    private BigDecimal profit;
    private String remark;
}
