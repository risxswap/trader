package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraderTaskSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TraderTaskSchedulerService.class);

    private final String taskType;
    private final Scheduler scheduler;

    public TraderTaskSchedulerService(String taskType, Scheduler scheduler) {
        this.taskType = taskType;
        this.scheduler = scheduler;
    }

    public void refresh(SystemTaskStatusEvent task) throws SchedulerException {
        if (Boolean.FALSE.equals(task.getEnabled())) {
            delete(task.getTaskCode());
            return;
        }

        JobDetail jobDetail = JobBuilder.newJob(TraderQuartzJob.class)
                .withIdentity(jobKey(task.getTaskCode()))
                .usingJobData("taskType", task.getTaskType())
                .usingJobData("taskCode", task.getTaskCode())
                .storeDurably(false)
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(task.getTaskCode()))
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCron()))
                .build();

        if (scheduler.checkExists(jobKey(task.getTaskCode()))) {
            scheduler.deleteJob(jobKey(task.getTaskCode()));
        }
        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void delete(String taskCode) throws SchedulerException {
        scheduler.deleteJob(jobKey(taskCode));
    }

    public void trigger(SystemTaskStatusEvent task) throws SchedulerException {
        JobKey jobKey = jobKey(task.getTaskCode());
        if (scheduler.checkExists(jobKey)) {
            log.info("Trigger quartz job for trader task taskType={} taskCode={} taskName={} mode=existing-job",
                    task.getTaskType(), task.getTaskCode(), task.getTaskName());
            scheduler.triggerJob(jobKey);
        } else {
            log.info("Trigger quartz job for trader task taskType={} taskCode={} taskName={} mode=create-durable-job",
                    task.getTaskType(), task.getTaskCode(), task.getTaskName());
            JobDetail jobDetail = JobBuilder.newJob(TraderQuartzJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("taskType", task.getTaskType())
                    .usingJobData("taskCode", task.getTaskCode())
                    .storeDurably(true)
                    .build();
            scheduler.addJob(jobDetail, true);
            scheduler.triggerJob(jobKey);
        }
    }

    JobKey jobKey(String taskCode) {
        return JobKey.jobKey(taskCode, taskType);
    }

    TriggerKey triggerKey(String taskCode) {
        return TriggerKey.triggerKey(taskCode, taskType);
    }
}
