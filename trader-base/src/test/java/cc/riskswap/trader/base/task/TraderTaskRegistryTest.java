package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraderTaskRegistryTest {

    @Test
    void should_register_unique_tasks() {
        TraderTask task = new SampleTask("fundSync");
        TraderTaskRegistry registry = new TraderTaskRegistry(List.of(task));
        assertEquals(task, registry.getTask("fundSync"));
    }

    @Test
    void should_fail_when_task_code_duplicated() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new TraderTaskRegistry(List.of(new SampleTask("dup"), new SampleTask("dup"))));
        assertEquals("Duplicate trader task code: dup", ex.getMessage());
    }

    private static class SampleTask implements TraderTask {
        private final String code;

        public SampleTask(String code) {
            this.code = code;
        }

        @Override
        public String getTaskCode() {
            return code;
        }

        @Override
        public String getTaskName() {
            return code;
        }

        @Override
        public boolean defaultEnabled() {
            return true;
        }

        @Override
        public String getParamSchema() {
            return "{\"type\":\"object\"}";
        }

        @Override
        public String getDefaultParams() {
            return "{}";
        }

        @Override
        public void execute(TraderTaskContext context) {
        }
    }
}
