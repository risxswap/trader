package cc.riskswap.trader.collector.system;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskStartupLogger {

    private final ApplicationContext applicationContext;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void logTaskSchedules() {
        log.info("\n{}", buildTaskStartupMessage());
    }

    String buildTaskStartupMessage() {
        List<TaskScheduleEntry> entries = collectTaskSchedules();
        if (entries.isEmpty()) {
            return "已注册定时任务:\n- 无";
        }
        StringBuilder builder = new StringBuilder("注册定时任务:\n");
        for (TaskScheduleEntry entry : entries) {
            builder.append("- ").append(entry.taskName()).append(": ").append(entry.cron()).append('\n');
        }
        return builder.toString().trim();
    }

    private List<TaskScheduleEntry> collectTaskSchedules() {
        List<TaskScheduleEntry> entries = new ArrayList<>();
        for (Object bean : applicationContext.getBeansOfType(Object.class).values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            ReflectionUtils.doWithMethods(targetClass, method -> appendTaskSchedule(entries, targetClass, method),
                    this::isScheduledTaskMethod);
        }
        entries.sort(Comparator.comparing(TaskScheduleEntry::taskName));
        return entries;
    }

    private boolean isScheduledTaskMethod(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, TraderTaskLog.class)
                && AnnotatedElementUtils.hasAnnotation(method, Scheduled.class);
    }

    private void appendTaskSchedule(List<TaskScheduleEntry> entries, Class<?> targetClass, Method method) {
        TraderTaskLog taskLog = AnnotatedElementUtils.findMergedAnnotation(method, TraderTaskLog.class);
        Scheduled scheduled = AnnotatedElementUtils.findMergedAnnotation(method, Scheduled.class);
        if (taskLog == null || scheduled == null) {
            return;
        }
        String taskName = StringUtils.hasText(taskLog.value())
                ? taskLog.value()
                : targetClass.getSimpleName() + "." + method.getName();
        entries.add(new TaskScheduleEntry(taskName, resolveCron(scheduled.cron())));
    }

    private String resolveCron(String cron) {
        if (cron == null || cron.isBlank()) {
            return "-";
        }
        return environment.resolvePlaceholders(cron);
    }

    private record TaskScheduleEntry(String taskName, String cron) {
    }
}
