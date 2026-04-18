package cc.riskswap.trader.admin.common.model;

import lombok.Data;
import java.time.Instant;

/**
 * 统一API响应格式
 *
 * @param <T> 响应数据类型
 */
@Data
public class ResData<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private Instant timestamp;

    /**
     * 私有构造函数
     */
    private ResData() {
        this.timestamp = Instant.now();
    }

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ResData<T> success(T data) {
        ResData<T> response = new ResData<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 成功响应（无数据）
     *
     * @return 成功响应
     */
    public static <T> ResData<T> success() {
        return success(null);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 成功响应
     */
    public static <T> ResData<T> success(String message, T data) {
        ResData<T> response = new ResData<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /**
     * 错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误响应
     */
    public static <T> ResData<T> error(Integer code, String message) {
        ResData<T> response = new ResData<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    /**
     * 错误响应（默认500错误码）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误响应
     */
    public static <T> ResData<T> error(String message) {
        return error(500, message);
    }

    /**
     * 客户端错误响应（400错误码）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 客户端错误响应
     */
    public static <T> ResData<T> badRequest(String message) {
        return error(400, message);
    }

    /**
     * 未找到错误响应（404错误码）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 未找到错误响应
     */
    public static <T> ResData<T> notFound(String message) {
        return error(404, message);
    }
}