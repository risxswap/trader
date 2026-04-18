package cc.riskswap.trader.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 任务执行日志记录注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TaskLogRecord {
    /**
     * 任务名称
     */
    String name();

    /**
     * 任务分组
     */
    String group() default "默认分组";
}
