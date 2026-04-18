package cc.riskswap.trader.base.logging;

import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class TraderThreadContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> parentContext = TraderThreadContext.snapshot();
        return () -> {
            Map<String, String> previous = TraderThreadContext.snapshot();
            TraderThreadContext.restore(parentContext);
            try {
                runnable.run();
            } finally {
                if (previous.isEmpty()) {
                    TraderThreadContext.clear();
                } else {
                    TraderThreadContext.restore(previous);
                }
            }
        };
    }
}
