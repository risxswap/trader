package cc.riskswap.trader.base.event;

import lombok.Data;

@Data
public class TraderStreamEvent {
    private String eventType;
    private String payloadJson;
}