package cc.riskswap.trader.base.dao.base.param;

import lombok.Data;

@Data
public class SystemTaskTriggerParam {
    private Long id;
    private String appName;
    private String taskCode;
    private String paramsJson;
}
