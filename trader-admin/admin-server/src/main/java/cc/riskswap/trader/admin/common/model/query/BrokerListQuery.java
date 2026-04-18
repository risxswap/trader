package cc.riskswap.trader.admin.common.model.query;

import lombok.Data;

@Data
public class BrokerListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String keyword;
}
