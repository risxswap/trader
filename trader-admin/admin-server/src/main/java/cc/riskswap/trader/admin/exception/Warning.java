package cc.riskswap.trader.admin.exception;

/**
 * 市场数据业务异常类
 * 用于封装业务逻辑中的异常情况
 *
 * @author Market Data Team
 * @since 1.0.0
 */
public class Warning extends RuntimeException {

    private final int code;

    public Warning(String message) {
        super(message);
        this.code = 500;
    }

    public Warning(int code, String message) {
        super(message);
        this.code = code;
    }

    public Warning(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public Warning(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}