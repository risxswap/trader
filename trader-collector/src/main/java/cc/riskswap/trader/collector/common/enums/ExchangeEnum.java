package cc.riskswap.trader.collector.common.enums;

public enum ExchangeEnum {

    SSE("SSE", "上交所"),
    SZSE("SZSE", "深交所"),
    CFFEX("CFFEX", "中金所"),
    SHFE("SHFE", "上期所"),
    DCE("DCE", "大商所"),
    CZCE("CZCE", "郑商所"),
    INE("INE", "上能源");

    public final String code;
    public final String name;

    ExchangeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
