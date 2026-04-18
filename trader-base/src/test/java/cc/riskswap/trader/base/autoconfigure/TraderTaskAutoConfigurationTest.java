package cc.riskswap.trader.base.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class TraderTaskAutoConfigurationTest {

    @Test
    void should_expose_application_context_to_quartz_jobs_via_scheduler_context() {
        TraderTaskAutoConfiguration configuration = new TraderTaskAutoConfiguration();
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        SchedulerFactoryBean factoryBean = configuration.traderTaskSchedulerFactoryBean(applicationContext);
        Object schedulerContextKey = ReflectionTestUtils.getField(factoryBean, "applicationContextSchedulerContextKey");

        assertEquals(
                "applicationContext",
                assertInstanceOf(String.class, schedulerContextKey)
        );
    }
}
