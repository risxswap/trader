package cc.riskswap.trader.admin.common.util;

/**
 * 任务执行上下文，使用 ThreadLocal 保证线程安全
 * 用于在任务执行过程中累加记录日志内容（如 Markdown 格式）
 */
public class TaskContextUtil {
    private static final ThreadLocal<StringBuilder> CONTEXT = ThreadLocal.withInitial(StringBuilder::new);

    /**
     * 追加内容
     * @param text 文本内容
     */
    public static void append(String text) {
        CONTEXT.get().append(text);
    }

    /**
     * 追加一行内容
     * @param text 文本内容
     */
    public static void appendLine(String text) {
        CONTEXT.get().append(text).append("\n");
    }

    /**
     * 获取当前上下文中的所有内容
     * @return 文本内容
     */
    public static String getContent() {
        return CONTEXT.get().toString();
    }

    /**
     * 清理上下文，防止内存泄漏
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
