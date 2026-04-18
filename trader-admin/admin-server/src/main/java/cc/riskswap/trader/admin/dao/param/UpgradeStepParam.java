package cc.riskswap.trader.admin.dao.param;

import lombok.Data;

@Data
public class UpgradeStepParam {
    private Integer orderNo;
    private String type;
    private String sqlText;
}
