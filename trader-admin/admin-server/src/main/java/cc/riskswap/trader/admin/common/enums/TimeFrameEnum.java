package cc.riskswap.trader.admin.common.enums;

public enum TimeFrameEnum {

    M1("1m"),
    M5("5m"),
    H1("1h"),
    D1("1d");

    public final String code;

    TimeFrameEnum(String code) {
        this.code = code;
    }
}
