package cc.riskswap.trader.executor.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradingStatusEnum {
    PENDING("PENDING", "进行中"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;
}
