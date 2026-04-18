package cc.riskswap.trader.admin.dao.query;

import lombok.Data;

@Data
public class FundQuery {

    private String name;

    private Integer pageNo;

    private Integer pageSize;
}
