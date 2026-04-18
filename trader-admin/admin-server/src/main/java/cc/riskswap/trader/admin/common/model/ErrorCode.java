package cc.riskswap.trader.admin.common.model;

public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "未找到"),
    CONFLICT(409, "冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    PARAM_INVALID(40001, "参数非法"),
    RESOURCE_NOT_FOUND(40004, "资源不存在"),
    MATRIX_FORBIDDEN(51001, "Matrix权限错误"),
    MATRIX_JOIN_REQUIRED(51002, "需加入房间"),
    MATRIX_UNKNOWN(51003, "Matrix未知错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
