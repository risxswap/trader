package cc.riskswap.trader.executor.pubsub.message;

import cc.riskswap.trader.executor.common.enums.MessageTypeEnum;
import lombok.Data;

@Data
public class BaseMessage {

    /**
     * 操作类型
     * @see MessageTypeEnum
     */
    protected String type;



}
