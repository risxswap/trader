package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;

import java.util.List;

@Data
public class UpgradeExecuteParam {
    private String version;
    private String title;
    private String description;
    private String operator;
    private Boolean dryRun;
    private List<UpgradeStepParam> steps;
}
