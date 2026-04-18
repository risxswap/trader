package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraderQuartzJobTest {

    @Test
    void should_resolve_application_context_from_scheduler_context_and_execute_task() throws Exception {
        org.quartz.JobExecutionContext jobExecutionContext = mock(org.quartz.JobExecutionContext.class);
        org.quartz.Scheduler scheduler = mock(org.quartz.Scheduler.class);
        org.quartz.SchedulerContext schedulerContext = new org.quartz.SchedulerContext();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TraderTaskExecutor traderTaskExecutor = mock(TraderTaskExecutor.class);

        org.quartz.JobDataMap jobDataMap = new org.quartz.JobDataMap();
        jobDataMap.put("taskType", "COLLECTOR");
        jobDataMap.put("taskCode", "fundSync");

        schedulerContext.put("applicationContext", applicationContext);

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
        when(jobExecutionContext.getScheduledFireTime()).thenReturn(new Date(1710000000000L));
        when(scheduler.getContext()).thenReturn(schedulerContext);
        when(applicationContext.getBean(TraderTaskExecutor.class)).thenReturn(traderTaskExecutor);

        new TraderQuartzJob().execute(jobExecutionContext);

        verify(traderTaskExecutor).execute("COLLECTOR", "fundSync", 1710000000L);
    }
}
