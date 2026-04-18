package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;

@Data
public class FundUpdateParam {
    private String name;
    private String status;
    private String market;
    private String exchange;
}
