package cc.riskswap.trader.executor.pubsub.publisher;

import org.springframework.stereotype.Component;

import cc.riskswap.trader.executor.common.enums.MessageTypeEnum;
import cc.riskswap.trader.executor.pubsub.Channels;
import cc.riskswap.trader.executor.pubsub.message.InvestmentLogMessage;

/**
 * 投资日志消息发布者
 */
@Component
public class InvestmentLogPublisher extends BasePublisher<InvestmentLogMessage> {

    private static final String CHANNEL = Channels.INVESTMENT_LOG.code;

    @Override
    protected String getChannel() {
        return CHANNEL;
    }

    public void create(Long id) {
        InvestmentLogMessage message = new InvestmentLogMessage();
        message.setType(MessageTypeEnum.CREATE.code);
        message.setId(id);
        publish(message);
    }
}
