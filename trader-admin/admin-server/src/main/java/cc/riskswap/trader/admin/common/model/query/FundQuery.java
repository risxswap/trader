package cc.riskswap.trader.admin.common.model.query;

import lombok.Data;

@Data
public class FundQuery {

    private String name;

    private Integer pageNo;

    private Integer pageSize;
}
