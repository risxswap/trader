package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;

@Data
public class UpgradeStepParam {
    private Integer orderNo;
    private String type;
    private String sqlText;
}
