package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class TraderTaskSchedulerServiceTest {

    @Test
    void should_log_when_triggering_existing_job(CapturedOutput output) throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        TraderTaskSchedulerService schedulerService = new TraderTaskSchedulerService("COLLECTOR", scheduler);
        SystemTaskStatusEvent task = new SystemTaskStatusEvent();
        task.setTaskType("COLLECTOR");
        task.setTaskCode("fundSync");
        task.setTaskName("同步基金");

        JobKey jobKey = JobKey.jobKey("fundSync", "COLLECTOR");
        when(scheduler.checkExists(jobKey)).thenReturn(true);

        schedulerService.trigger(task);

        verify(scheduler).triggerJob(jobKey);
        assertThat(output.getOut())
                .contains("Trigger quartz job for trader task")
                .contains("fundSync")
                .contains("COLLECTOR");
    }
}
