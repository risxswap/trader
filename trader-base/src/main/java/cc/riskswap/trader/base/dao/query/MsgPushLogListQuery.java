package cc.riskswap.trader.base.dao.query;

import lombok.Data;

@Data
public class MsgPushLogListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String type;
    private String channel;
    private String status;
}
