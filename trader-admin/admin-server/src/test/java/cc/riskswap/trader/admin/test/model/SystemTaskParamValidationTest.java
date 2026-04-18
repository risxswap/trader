package cc.riskswap.trader.admin.test.model;

import cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceCreateParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskUpdateParam;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SystemTaskParamValidationTest {

    @Test
    void should_require_task_create_fields() throws Exception {
        Assertions.assertNotNull(SystemTaskInstanceCreateParam.class.getDeclaredField("taskType").getAnnotation(NotBlank.class));
        Assertions.assertNotNull(SystemTaskInstanceCreateParam.class.getDeclaredField("taskCode").getAnnotation(NotBlank.class));
        Assertions.assertNotNull(SystemTaskInstanceCreateParam.class.getDeclaredField("taskName").getAnnotation(NotBlank.class));
        Assertions.assertNotNull(SystemTaskInstanceCreateParam.class.getDeclaredField("cron").getAnnotation(NotBlank.class));
        Assertions.assertNotNull(SystemTaskInstanceCreateParam.class.getDeclaredField("status").getAnnotation(NotBlank.class));
    }

    @Test
    void should_require_id_cron_and_status_for_update_param() throws Exception {
        Assertions.assertNotNull(SystemTaskUpdateParam.class.getDeclaredField("id").getAnnotation(NotNull.class));
        Assertions.assertNotNull(SystemTaskUpdateParam.class.getDeclaredField("cron").getAnnotation(NotBlank.class));
        Assertions.assertNotNull(SystemTaskUpdateParam.class.getDeclaredField("status").getAnnotation(NotBlank.class));
    }
}
