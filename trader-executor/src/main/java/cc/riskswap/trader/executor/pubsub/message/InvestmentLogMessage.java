package cc.riskswap.trader.executor.pubsub.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class InvestmentLogMessage extends BaseMessage {

    /**
     * 投资ID
     */
    private Long id;
}
