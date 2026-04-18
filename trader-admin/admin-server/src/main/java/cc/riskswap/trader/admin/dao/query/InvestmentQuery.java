package cc.riskswap.trader.admin.dao.query;

import cc.riskswap.trader.admin.dao.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 投资查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvestmentQuery extends PageDto<Object> {
    private String name;
    private String groupName;
    private String status;
    private String strategy;
    private Double budget;
}
