package cc.riskswap.trader.admin.pubsub.publisher;

import org.springframework.stereotype.Component;

import cc.riskswap.trader.admin.common.enums.MessageTypeEnum;
import cc.riskswap.trader.admin.pubsub.Channels;
import cc.riskswap.trader.admin.pubsub.message.InvestmentMessage;

/**
 * 投资消息发布者
 */
@Component
public class InvestmentPublisher extends BasePublisher<InvestmentMessage> {

    private static final String CHANNEL = Channels.INVESTMENT.code;

    @Override
    protected String getChannel() {
        return CHANNEL;
    }

    public void update(Integer id) {
        InvestmentMessage message = new InvestmentMessage();
        message.setType(MessageTypeEnum.UPDATE.code);
        message.setId(id);
        publish(message);
    }

    public void delete(Integer id) {
        InvestmentMessage message = new InvestmentMessage();
        message.setType(MessageTypeEnum.DELETE.code);
        message.setId(id);
        publish(message);
    }

    public void create(Integer id) {
        InvestmentMessage message = new InvestmentMessage();
        message.setType(MessageTypeEnum.CREATE.code);
        message.setId(id);
        publish(message);
    }
}
