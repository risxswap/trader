package cc.riskswap.trader.admin.common.model.query;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 投资日志查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvestmentLogQuery extends PageDto<Object> {
    private Integer investmentId;
    private String type;
}
