package cc.riskswap.trader.admin.pubsub.subscriber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cc.riskswap.trader.admin.pubsub.Channels;
import cc.riskswap.trader.admin.pubsub.message.InvestmentMessage;
import cc.riskswap.trader.admin.service.InvestmentLogService;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InvestmentLogSubscriber implements BaseSubscriber {

    @Autowired
    private InvestmentLogService investmentLogService;

    @Override
    public String getChannel() {
        return Channels.INVESTMENT_LOG.code;
    }

    @Override
    public void processMessage(String message) {
        InvestmentMessage investmentMessage = JSONUtil.parseObj(message).toBean(InvestmentMessage.class);
        log.info("Received message from channel: {}, content: {}", getChannel(), JSONUtil.toJsonStr(investmentMessage));
        
        try {
            if (investmentMessage.getId() != null) {
                investmentLogService.notifyInvestmentLog(investmentMessage.getId());
            }
        } catch (Exception e) {
            log.error("Failed to notify investment log", e);
        }
    }

}
