package cc.riskswap.trader.collector.test.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.Scheduled;

import cc.riskswap.trader.base.logging.TraderTaskLog;

class TaskStartupLoggerTest {

    @Test
    void buildsStartupMessageWithResolvedCronExpressions() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("demo.task.cron", "0 15 2 * * ?");

        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);
        context.registerBean("demoTask", DemoTask.class);
        context.refresh();

        Class<?> loggerClass = Class.forName("cc.riskswap.trader.collector.system.TaskStartupLogger");
        Object logger = loggerClass
                .getConstructor(org.springframework.context.ApplicationContext.class, org.springframework.core.env.Environment.class)
                .newInstance(context, environment);
        Method method = loggerClass.getDeclaredMethod("buildTaskStartupMessage");
        method.setAccessible(true);

        String message = (String) method.invoke(logger);

        assertTrue(message.contains("示例任务: 0 15 2 * * ?"));
        assertFalse(message.contains("忽略任务"));
        context.close();
    }

    @Test
    void exposesApplicationReadyEventListener() throws Exception {
        Class<?> loggerClass = Class.forName("cc.riskswap.trader.collector.system.TaskStartupLogger");
        Method method = loggerClass.getDeclaredMethod("logTaskSchedules");

        assertTrue(method.isAnnotationPresent(org.springframework.context.event.EventListener.class));
    }

    static class DemoTask {

        @TraderTaskLog("示例任务")
        @Scheduled(cron = "${demo.task.cron:0 0 1 * * ?}", scheduler = "demoTaskScheduler")
        void syncDemo() {
        }

        @Scheduled(cron = "0 0 3 * * ?")
        void ignoredTask() {
        }
    }
}
