package cc.riskswap.trader.admin.dao.query;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvestmentListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String name;
    private String groupName;
    private String status;
    private String strategy;
    private BigDecimal budget;
}
