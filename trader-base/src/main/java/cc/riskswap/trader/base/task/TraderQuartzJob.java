package cc.riskswap.trader.base.task;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class TraderQuartzJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(TraderQuartzJob.class);

    @Override
    public void execute(JobExecutionContext context) throws RuntimeException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String taskType = dataMap.getString("taskType");
        String taskCode = dataMap.getString("taskCode");
        long fireTimeEpochSec = context.getScheduledFireTime() == null ? System.currentTimeMillis() / 1000 : context.getScheduledFireTime().getTime() / 1000;
        try {
            ApplicationContext applicationContext = (ApplicationContext) context.getScheduler().getContext().get("applicationContext");
            TraderTaskExecutor executor = applicationContext.getBean(TraderTaskExecutor.class);
            log.info("Start quartz task dispatch taskType={} taskCode={} fireTimeEpochSec={}", taskType, taskCode, fireTimeEpochSec);
            executor.execute(taskType, taskCode, fireTimeEpochSec);
        } catch (Exception e) {
            log.error("Quartz task dispatch failed taskType={} taskCode={} fireTimeEpochSec={}", taskType, taskCode, fireTimeEpochSec, e);
            throw new RuntimeException(e);
        }
    }
}
