package cc.riskswap.trader.executor.common.enums;


public enum MessageTypeEnum {
    CREATE("create", "创建"),
    UPDATE("update", "更新"),
    DELETE("delete", "删除"),
    BATCH_CREATE("batch_create", "批量创建"),
    BATCH_UPDATE("batch_update", "批量更新"),
    BATCH_DELETE("batch_delete", "批量删除"),
    ;

    public final String code;

    public final String name;

    MessageTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
