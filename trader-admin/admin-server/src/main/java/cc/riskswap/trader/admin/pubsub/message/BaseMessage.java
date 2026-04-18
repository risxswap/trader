package cc.riskswap.trader.admin.pubsub.message;

import cc.riskswap.trader.admin.common.enums.MessageTypeEnum;
import lombok.Data;

/**
 * 基础消息
 */
@Data
public class BaseMessage {
    /**
     * 消息类型
     * @see MessageTypeEnum
     */
    private String type;
}
