package cc.riskswap.trader.base.logging;

public final class TaskLogExecutionContext {

    private static final ThreadLocal<Boolean> EXECUTOR_MANAGED = ThreadLocal.withInitial(() -> false);

    private TaskLogExecutionContext() {
    }

    public static void markExecutorManaged(boolean managed) {
        EXECUTOR_MANAGED.set(managed);
    }

    public static boolean isExecutorManaged() {
        return Boolean.TRUE.equals(EXECUTOR_MANAGED.get());
    }

    public static void clear() {
        EXECUTOR_MANAGED.remove();
    }
}
