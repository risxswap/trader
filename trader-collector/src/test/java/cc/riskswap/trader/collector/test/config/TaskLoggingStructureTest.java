package cc.riskswap.trader.collector.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.task.CalendarSyncTask;
import cc.riskswap.trader.collector.task.FundAdjSyncTask;
import cc.riskswap.trader.collector.task.FundMarketSyncTask;
import cc.riskswap.trader.collector.task.FundNavSyncTask;
import cc.riskswap.trader.collector.task.FundSyncTask;

class TaskLoggingStructureTest {

    @Test
    void taskMethodsDeclareTaskLogMetadata() throws Exception {
        Class<? extends Annotation> annotationClass = Class.forName("cc.riskswap.trader.base.logging.TraderTaskLog")
                .asSubclass(Annotation.class);

        assertTaskMetadata(FundSyncTask.class.getDeclaredMethod("execute", TraderTaskContext.class), annotationClass, "基金同步任务", false);
        assertTaskMetadata(FundAdjSyncTask.class.getDeclaredMethod("execute", TraderTaskContext.class), annotationClass, "基金复权同步任务", false);
        assertTaskMetadata(FundNavSyncTask.class.getDeclaredMethod("execute", TraderTaskContext.class), annotationClass, "基金净值同步任务", false);
        assertTaskMetadata(FundMarketSyncTask.class.getDeclaredMethod("execute", TraderTaskContext.class), annotationClass, "基金行情同步任务", false);
        assertTaskMetadata(CalendarSyncTask.class.getDeclaredMethod("execute", TraderTaskContext.class), annotationClass, "交易日历同步任务", false);
    }

    @Test
    void removesLocalTaskLogPersistenceImplementation() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.common.annotations.TaskLog"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.aspect.TaskLogAspect"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.repository.dao.TaskLogDao"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.repository.dao.mapper.TaskLogMapper"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.repository.entity.TaskLog"));
    }

    @Test
    void definesThreadLocalTaskContentContext() throws Exception {
        Class<?> contextClass = Class.forName("cc.riskswap.trader.collector.common.util.TaskContentContext");

        assertNotNull(contextClass.getDeclaredMethod("start"));
        assertNotNull(contextClass.getDeclaredMethod("current"));
        assertNotNull(contextClass.getDeclaredMethod("clear"));
        assertNotNull(contextClass.getDeclaredMethod("addMetric", String.class, long.class));
        assertNotNull(contextClass.getDeclaredMethod("addAttribute", String.class, String.class));
        assertNotNull(contextClass.getDeclaredMethod("addDetail", String.class, String.class));
        assertNotNull(contextClass.getDeclaredMethod("addError", String.class));
        assertNotNull(Class.forName("cc.riskswap.trader.collector.common.util.TaskContentContext$Snapshot"));
    }

    private void assertTaskMetadata(Method method,
                                    Class<? extends Annotation> annotationClass,
                                    String taskName,
                                    boolean shouldBeScheduled) throws Exception {
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        if (shouldBeScheduled) {
            assertNotNull(scheduled);
        } else {
            assertEquals(null, scheduled);
        }

        Annotation annotation = method.getAnnotation(annotationClass);
        assertNotNull(annotation);
        assertEquals(taskName, annotationClass.getDeclaredMethod("value").invoke(annotation));
    }
}
