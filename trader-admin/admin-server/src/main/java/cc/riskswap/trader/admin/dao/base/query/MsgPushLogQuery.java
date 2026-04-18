package cc.riskswap.trader.admin.dao.base.query;

import cc.riskswap.trader.admin.dao.base.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MsgPushLogQuery extends PageDto<Object> {
    private String type;
    private String channel;
    private String status;
}
