package cc.riskswap.trader.collector.common.enums;

public enum FundMarketEnum {
    ETF("E", "场内基金"),
    OMF("O", "场外基金");

    public final String code;

    public final String desc;


    FundMarketEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
