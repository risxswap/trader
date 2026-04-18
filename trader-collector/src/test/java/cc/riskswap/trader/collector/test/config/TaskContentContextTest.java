package cc.riskswap.trader.collector.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cc.riskswap.trader.collector.common.util.TaskContentContext;

class TaskContentContextTest {

    @Test
    void accumulatesSnapshotWithoutReturnValueCoupling() {
        TaskContentContext.start();
        try {
            TaskContentContext.addAttribute("同步区间", "2026-04-01 ~ 2026-04-11");
            TaskContentContext.addMetric("拉取记录数", 12);
            TaskContentContext.addMetric("拉取记录数", 8);
            TaskContentContext.addDetail("基金净值同步", "2026-04-10 原始=10,去重后=9,批次=1");
            TaskContentContext.addError("示例异常");

            TaskContentContext.Snapshot snapshot = TaskContentContext.current();
            assertEquals("2026-04-01 ~ 2026-04-11", snapshot.getAttributes().get("同步区间"));
            assertEquals(20L, snapshot.getMetrics().get("拉取记录数"));
            assertEquals("2026-04-10 原始=10,去重后=9,批次=1", snapshot.getDetails().get("基金净值同步").get(0));
            assertEquals("示例异常", snapshot.getErrors().get(0));
        } finally {
            TaskContentContext.clear();
        }

        assertTrue(TaskContentContext.current().getMetrics().isEmpty());
    }
}
