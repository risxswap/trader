package cc.riskswap.trader.collector.aspect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import cc.riskswap.trader.collector.common.model.ResData;
import cc.riskswap.trader.collector.exception.Warning;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的各种异常，返回标准化的错误响应
 *
 * @author Market Data Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAspect {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(Warning.class)
    public ResponseEntity<ResData<Void>> handleMarketDataException(Warning e) {
        log.warn("业务异常: {}", e.getMessage(), e);
        return ResponseEntity.status(e.getCode())
                .body(ResData.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理参数校验异常 - @RequestBody 参数校验
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResData<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        return ResponseEntity.badRequest()
                .body(ResData.error(400, "参数校验失败: " + message));
    }

    /**
     * 处理参数绑定异常 - @ModelAttribute 参数校验
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResData<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常: {}", message);
        return ResponseEntity.badRequest()
                .body(ResData.error(400, "参数绑定失败: " + message));
    }

    /**
     * 处理约束违反异常 - @RequestParam 参数校验
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResData<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束违反异常: {}", message);
        return ResponseEntity.badRequest()
                .body(ResData.error(400, "参数校验失败: " + message));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResData<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 类型不匹配，期望类型: %s", 
                e.getName(), e.getRequiredType().getSimpleName());
        log.warn("参数类型不匹配异常: {}", message);
        return ResponseEntity.badRequest()
                .body(ResData.error(400, message));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResData<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ResData.error(400, "参数错误: " + e.getMessage()));
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResData<Void>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResData.error(500, "系统内部错误"));
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResData<Void>> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResData.error(500, "系统异常，请联系管理员"));
    }
}