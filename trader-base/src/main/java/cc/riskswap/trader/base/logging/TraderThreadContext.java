package cc.riskswap.trader.base.logging;

import java.util.HashMap;
import java.util.Map;

public final class TraderThreadContext {

    private static final String TRACE_ID = "traceId";
    private static final String TASK_NAME = "taskName";
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    private TraderThreadContext() {
    }

    public static void put(String key, String value) {
        if (value == null) {
            CONTEXT.get().remove(key);
            return;
        }
        CONTEXT.get().put(key, value);
    }

    public static String get(String key) {
        return CONTEXT.get().get(key);
    }

    public static void setTraceId(String traceId) {
        put(TRACE_ID, traceId);
    }

    public static String getTraceId() {
        return get(TRACE_ID);
    }

    public static void setTaskName(String taskName) {
        put(TASK_NAME, taskName);
    }

    public static String getTaskName() {
        return get(TASK_NAME);
    }

    public static Map<String, String> snapshot() {
        return new HashMap<>(CONTEXT.get());
    }

    public static void restore(Map<String, String> context) {
        CONTEXT.set(new HashMap<>(context));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
