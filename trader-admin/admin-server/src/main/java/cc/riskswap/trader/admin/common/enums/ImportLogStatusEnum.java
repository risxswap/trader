package cc.riskswap.trader.admin.common.enums;

public enum ImportLogStatusEnum {

    SUCCESS("success"),
    FAILED("failed");

    public final String code;

    ImportLogStatusEnum(String code) {
        this.code = code;
    }
}
