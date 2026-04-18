package cc.riskswap.trader.admin.dao.base.param;

import lombok.Data;

@Data
public class SystemTaskUpdateParam {
    private Long id;
    private String cron;
    private String status;
    private String paramsJson;
    private String remark;
}
