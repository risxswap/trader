package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskExecutionReportTest {

    @Test
    void should_accumulate_counts_and_message() {
        TaskExecutionReport report = new TaskExecutionReport();
        report.addSynced(10);
        report.addFailed(2);
        report.setMessage("running");
        assertEquals(10L, report.getSyncedCount());
        assertEquals(2L, report.getFailedCount());
        assertEquals("running", report.getMessage());
    }
}

