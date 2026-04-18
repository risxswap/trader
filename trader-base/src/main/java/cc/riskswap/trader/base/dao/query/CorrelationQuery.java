package cc.riskswap.trader.base.dao.query;

import cc.riskswap.trader.base.dao.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 证券相关性查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CorrelationQuery extends PageDto<Object> {
    private String asset1;
    private String asset2;
    private String period;
    private BigDecimal minCoefficient;
    private BigDecimal maxCoefficient;
}
