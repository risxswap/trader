package cc.riskswap.trader.base.dao.query;

import lombok.Data;

@Data
public class ExchangeListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String keyword;
}
