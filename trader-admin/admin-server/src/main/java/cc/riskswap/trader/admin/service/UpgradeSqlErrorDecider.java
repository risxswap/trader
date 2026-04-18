package cc.riskswap.trader.admin.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UpgradeSqlErrorDecider {

    public boolean isIgnorable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("duplicate column name")
                        || normalized.contains("duplicate key name")
                        || normalized.contains("already exists")
                        || normalized.contains("multiple primary key defined")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        String fallback = null;
        while (current != null) {
            if (StrUtil.isNotBlank(current.getMessage())) {
                fallback = current.getMessage();
            }
            current = current.getCause();
        }
        return StrUtil.isNotBlank(fallback) ? fallback : throwable.getClass().getSimpleName();
    }
}
