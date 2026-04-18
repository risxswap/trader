package cc.riskswap.trader.executor.pubsub.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class InvestmentMessage extends BaseMessage {


    private Integer id;
}
