package cc.riskswap.trader.executor.common.enums;

public enum InvestmentStatusEnum {
    
    DRAFT("DRAFT", "草稿"),
    NORMAL("NORMAL", "正常"),
    RUNNING("RUNNING", "运行中"),
    STOPPED("STOPPED", "已停止"),
    ARCHIVED("ARCHIVED", "已归档");

    public final String code;

    public final String name;

    InvestmentStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static InvestmentStatusEnum getByCode(String code) {
        for (InvestmentStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
