package cc.riskswap.trader.base.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@Aspect
public class TraderTaskLogAspect {

    private static final Logger log = LoggerFactory.getLogger(TraderTaskLogAspect.class);
    private final TaskLogStore taskLogStore;

    public TraderTaskLogAspect(@Nullable TaskLogStore taskLogStore) {
        this.taskLogStore = taskLogStore;
    }

    @Around("@annotation(traderTaskLog)")
    public Object around(ProceedingJoinPoint joinPoint, TraderTaskLog traderTaskLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String taskName = StringUtils.hasText(traderTaskLog.value())
                ? traderTaskLog.value()
                : signature.getDeclaringType().getSimpleName() + "." + signature.getMethod().getName();
        String taskGroup = signature.getDeclaringType().getSimpleName();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        Map<String, String> previousContext = TraderThreadContext.snapshot();
        StopWatch stopWatch = new StopWatch();
        LocalDateTime startTime = LocalDateTime.now();
        persistRunningLog(taskName, taskGroup, startTime, traceId);

        TraderThreadContext.setTraceId(traceId);
        TraderThreadContext.setTaskName(taskName);
        MDC.put("traceId", traceId);
        MDC.put("taskName", taskName);

        stopWatch.start();
        try {
            if (traderTaskLog.logArguments()) {
                log.info("Task start taskName={} traceId={} args={}", taskName, traceId, Arrays.toString(joinPoint.getArgs()));
            } else {
                log.info("Task start taskName={} traceId={}", taskName, traceId);
            }
            Object result = joinPoint.proceed();
            stopWatch.stop();
            log.info("Task finish taskName={} traceId={} costMs={}", taskName, traceId, stopWatch.getTotalTimeMillis());
            persistSuccessLog(signature, taskName, taskGroup, traceId, joinPoint.getArgs(), traderTaskLog.logArguments(), result, startTime, stopWatch.getTotalTimeMillis());
            return result;
        } catch (Throwable throwable) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.error("Task fail taskName={} traceId={} costMs={}", taskName, traceId, stopWatch.getTotalTimeMillis(), throwable);
            persistFailureLog(signature, taskName, taskGroup, traceId, joinPoint.getArgs(), traderTaskLog.logArguments(), throwable, startTime, stopWatch.getTotalTimeMillis());
            throw throwable;
        } finally {
            MDC.remove("traceId");
            MDC.remove("taskName");
            TraderThreadContext.restore(previousContext);
            if (previousContext.isEmpty()) {
                TraderThreadContext.clear();
            }
        }
    }

    private void persistRunningLog(String taskName, String taskGroup, LocalDateTime startTime, String traceId) {
        if (TaskLogExecutionContext.isExecutorManaged()) {
            return;
        }
        try {
            if (taskLogStore != null) {
                taskLogStore.writeRunning(taskName, taskGroup, startTime, traceId);
            }
        } catch (Exception exception) {
            log.error("Persist task start log failed taskName={}", taskName, exception);
        }
    }

    private void persistSuccessLog(MethodSignature signature,
                                   String taskName,
                                   String taskGroup,
                                   String traceId,
                                   Object[] args,
                                   boolean logArguments,
                                   Object result,
                                   LocalDateTime startTime,
                                   long executionMs) {
        if (TaskLogExecutionContext.isExecutorManaged()) {
            return;
        }
        try {
            if (taskLogStore != null) {
                taskLogStore.writeFinished(
                        traceId,
                        "SUCCESS",
                        executionMs,
                        buildFinishedContent(signature, taskName, taskGroup, traceId, args, logArguments, startTime, "SUCCESS", executionMs, result, null)
                );
            }
        } catch (Exception exception) {
            log.error("Persist task success log failed taskName={} traceId={}", taskName, traceId, exception);
        }
    }

    private void persistFailureLog(MethodSignature signature,
                                   String taskName,
                                   String taskGroup,
                                   String traceId,
                                   Object[] args,
                                   boolean logArguments,
                                   Throwable throwable,
                                   LocalDateTime startTime,
                                   long executionMs) {
        if (TaskLogExecutionContext.isExecutorManaged()) {
            return;
        }
        try {
            if (taskLogStore != null) {
                taskLogStore.writeFinished(
                        traceId,
                        "FAILED",
                        executionMs,
                        buildFinishedContent(signature, taskName, taskGroup, traceId, args, logArguments, startTime, "FAILED", executionMs, null, throwable)
                );
            }
        } catch (Exception exception) {
            log.error("Persist task failure log failed taskName={} traceId={}", taskName, traceId, exception);
        }
    }



    private String buildFinishedContent(MethodSignature signature,
                                        String taskName,
                                        String taskGroup,
                                        String traceId,
                                        Object[] args,
                                        boolean logArguments,
                                        LocalDateTime startTime,
                                        String status,
                                        Long executionMs,
                                        Object result,
                                        Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Task Log\n\n");
        builder.append("- taskName: ").append(taskName).append('\n');
        builder.append("- taskGroup: ").append(taskGroup).append('\n');
        builder.append("- traceId: ").append(traceId).append('\n');
        builder.append("- method: ").append(signature.toShortString()).append('\n');
        builder.append("- status: ").append(status).append('\n');
        builder.append("- startTime: ").append(startTime).append('\n');
        if (executionMs != null) {
            builder.append("- executionMs: ").append(executionMs).append('\n');
        }
        builder.append('\n');
        builder.append("## Arguments\n\n");
        builder.append(logArguments ? Arrays.toString(args) : "[hidden]").append('\n');
        if (result != null) {
            builder.append('\n');
            builder.append("## Result\n\n");
            builder.append(String.valueOf(result)).append('\n');
        }
        if (throwable != null) {
            builder.append('\n');
            builder.append("## Error\n\n");
            builder.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append('\n');
        }
        return builder.toString();
    }
}
