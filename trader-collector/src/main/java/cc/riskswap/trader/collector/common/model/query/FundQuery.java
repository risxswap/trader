package cc.riskswap.trader.collector.common.model.query;

import lombok.Data;

@Data
public class FundQuery {

    private String name;

    private Integer pageNo;

    private Integer pageSize;
}
