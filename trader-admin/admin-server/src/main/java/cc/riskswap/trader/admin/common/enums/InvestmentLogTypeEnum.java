package cc.riskswap.trader.admin.common.enums;

public enum InvestmentLogTypeEnum {
    DEPOSIT("DEPOSIT", "入金"),
    WITHDRAWAL("WITHDRAWAL", "出金"),
    TRADE("TRADE", "交易"),
    ;

    public final String code;
    public final String name;

    InvestmentLogTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
