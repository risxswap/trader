package cc.riskswap.trader.base.dao.param;

import lombok.Data;

@Data
public class UpgradeStepParam {
    private Integer orderNo;
    private String type;
    private String sqlText;
}
