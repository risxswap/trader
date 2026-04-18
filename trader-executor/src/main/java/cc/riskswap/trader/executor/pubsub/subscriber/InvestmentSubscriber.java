package cc.riskswap.trader.executor.pubsub.subscriber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cc.riskswap.trader.executor.pubsub.message.InvestmentMessage;
import cc.riskswap.trader.executor.service.InvestmentService;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InvestmentSubscriber implements BaseSubscriber {

    @Autowired
    private InvestmentService investmentService;

    @Override
    public String getChannel() {
        return "investment";
    }

    @Override
    public void processMessage(String message) {
        InvestmentMessage investmentMessage = JSONUtil.parseObj(message).toBean(InvestmentMessage.class);
        log.info("Received message from channel: {}, content: {}", getChannel(), JSONUtil.toJsonStr(investmentMessage));
        // Since investment scheduling is handled by trader-base and admin, 
        // we no longer manually check and schedule investments here.
    }

}
