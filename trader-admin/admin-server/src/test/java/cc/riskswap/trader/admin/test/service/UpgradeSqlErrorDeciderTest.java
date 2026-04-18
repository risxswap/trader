package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.service.UpgradeSqlErrorDecider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UpgradeSqlErrorDeciderTest {

    private final UpgradeSqlErrorDecider decider = new UpgradeSqlErrorDecider();

    @Test
    void should_identify_ignorable_sql_errors_by_message() {
        Assertions.assertTrue(decider.isIgnorable(new RuntimeException("Duplicate column name 'enabled'")));
        Assertions.assertTrue(decider.isIgnorable(new RuntimeException("Duplicate key name 'idx_task_code'")));
        Assertions.assertTrue(decider.isIgnorable(new RuntimeException("Table 'system_task' already exists")));
        Assertions.assertTrue(decider.isIgnorable(new RuntimeException("Multiple primary key defined")));
    }

    @Test
    void should_extract_nested_error_message() {
        RuntimeException error = new RuntimeException(new IllegalStateException("Duplicate column name 'enabled'"));
        Assertions.assertEquals("Duplicate column name 'enabled'", decider.extractMessage(error));
    }

    @Test
    void should_reject_non_ignorable_error() {
        Assertions.assertFalse(decider.isIgnorable(new RuntimeException("Syntax error near 'ALTER'")));
    }
}
