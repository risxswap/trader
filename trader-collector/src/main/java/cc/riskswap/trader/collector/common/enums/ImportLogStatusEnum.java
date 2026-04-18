package cc.riskswap.trader.collector.common.enums;

public enum ImportLogStatusEnum {

    SUCCESS("success"),
    FAILED("failed");

    public final String code;

    ImportLogStatusEnum(String code) {
        this.code = code;
    }
}
