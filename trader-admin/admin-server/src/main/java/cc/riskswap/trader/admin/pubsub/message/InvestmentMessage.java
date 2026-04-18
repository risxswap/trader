package cc.riskswap.trader.admin.pubsub.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 投资消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvestmentMessage extends BaseMessage {
    
    private Integer id;
}
