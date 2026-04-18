package cc.riskswap.trader.admin.aspect;

import cc.riskswap.trader.admin.common.annotation.TaskLogRecord;
import cc.riskswap.trader.admin.common.util.TaskContextUtil;
import cc.riskswap.trader.admin.service.TaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 任务执行日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TaskLogAspect {

    private final TaskLogService taskLogService;

    @Around("@annotation(taskLogRecord)")
    public Object around(ProceedingJoinPoint joinPoint, TaskLogRecord taskLogRecord) throws Throwable {
        Long logId = null;
        try {
            logId = taskLogService.startTask(taskLogRecord.name(), taskLogRecord.group());
        } catch (Exception e) {
            log.error("Failed to create task log start record for {}", taskLogRecord.name(), e);
        }

        // 初始化上下文
        TaskContextUtil.clear();

        try {
            // 执行被注解的方法
            Object result = joinPoint.proceed();

            // 成功结束任务，并将上下文的内容记录到日志中
            if (logId != null) {
                taskLogService.finishTask(logId, true, TaskContextUtil.getContent(), null);
            }
            return result;
        } catch (Throwable e) {
            log.error("Task [{}] execution error", taskLogRecord.name(), e);
            // 发生异常，将异常信息及上下文中已累加的内容记录到日志中
            if (logId != null) {
                taskLogService.finishTask(logId, false, TaskContextUtil.getContent(), e.getMessage());
            }
            throw e;
        } finally {
            // 务必清理上下文，防止内存泄漏以及数据污染
            TaskContextUtil.clear();
        }
    }
}
