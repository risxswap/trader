package cc.riskswap.trader.admin.dao.query;

import lombok.Data;

@Data
public class InvestmentTradingListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private Integer investmentId;
    private Integer investmentLogId;
    private String asset;
}
