package cc.riskswap.trader.statistic.task;

import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.statistic.service.CorrelationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

class CorrelationTaskTest {

    @Test
    void should_expose_schema_and_default_params_for_min_abs_correlation() {
        CorrelationTask task = new CorrelationTask();

        Assertions.assertTrue(task.getParamSchema().contains("\"type\":\"object\""));
        Assertions.assertTrue(task.getParamSchema().contains("minAbsCorrelation"));
        Assertions.assertTrue(task.getParamSchema().contains("\"type\":\"number\""));
        Assertions.assertTrue(task.getParamSchema().contains("\"minimum\":0"));
        Assertions.assertTrue(task.getParamSchema().contains("\"maximum\":1"));
        Assertions.assertTrue(task.getDefaultParams().contains("\"minAbsCorrelation\":0.5"));
    }

    @Test
    void should_fallback_to_default_threshold_when_params_are_blank_or_invalid() {
        Assertions.assertEquals(0.5d, CorrelationTaskParams.fromJson(null).minAbsCorrelation());
        Assertions.assertEquals(0.5d, CorrelationTaskParams.fromJson("{}").minAbsCorrelation());
        Assertions.assertEquals(0.5d, CorrelationTaskParams.fromJson("{\"minAbsCorrelation\":2}").minAbsCorrelation());
        Assertions.assertEquals(0.5d, CorrelationTaskParams.fromJson("{\"minAbsCorrelation\":\"bad\"}").minAbsCorrelation());
    }

    @Test
    void should_call_batch_calculation_and_cleanup_once_with_default_threshold() throws Exception {
        Assertions.assertDoesNotThrow(() ->
                CorrelationService.class.getMethod("calculateAndSaveBatch", List.class, String.class, double.class));

        FundDao fundDao = Mockito.mock(FundDao.class);
        CorrelationService correlationService = Mockito.mock(CorrelationService.class);
        CorrelationTask task = createTask(fundDao, correlationService);

        Mockito.when(fundDao.listAll()).thenReturn(List.of(
                fund("510300"),
                fund("159915"),
                fund("510300"),
                fund(null)
        ));
        Mockito.when(correlationService.calculateAndSaveBatch(Mockito.anyList(), Mockito.eq("1Y"), Mockito.eq(0.5d))).thenReturn(1);
        Mockito.when(correlationService.cleanupHistoricalCorrelations()).thenReturn(0);

        task.doCalculateAllCorrelations(0.5d);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Fund>> fundsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(correlationService).calculateAndSaveBatch(fundsCaptor.capture(), Mockito.eq("1Y"), Mockito.eq(0.5d));
        Mockito.verify(correlationService, Mockito.never())
                .calculateAndSave(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(correlationService).cleanupHistoricalCorrelations();
        Assertions.assertEquals(2, fundsCaptor.getValue().size());
    }

    @Test
    void should_pass_threshold_from_context_to_service() throws Exception {
        FundDao fundDao = Mockito.mock(FundDao.class);
        CorrelationService correlationService = Mockito.mock(CorrelationService.class);
        CorrelationTask task = createTask(fundDao, correlationService);
        TraderTaskContext context = new TraderTaskContext();
        context.setParamsJson("{\"minAbsCorrelation\":0.8}");

        Mockito.when(fundDao.listAll()).thenReturn(List.of(
                fund("510300"),
                fund("159915")
        ));
        Mockito.when(correlationService.calculateAndSaveBatch(Mockito.anyList(), Mockito.eq("1Y"), Mockito.eq(0.8d))).thenReturn(0);
        Mockito.when(correlationService.cleanupHistoricalCorrelations()).thenReturn(0);

        task.execute(context);

        Mockito.verify(correlationService).calculateAndSaveBatch(Mockito.anyList(), Mockito.eq("1Y"), Mockito.eq(0.8d));
    }

    private CorrelationTask createTask(FundDao fundDao, CorrelationService correlationService) throws Exception {
        CorrelationTask task = new CorrelationTask();
        setField(task, "fundDao", fundDao);
        setField(task, "correlationService", correlationService);
        return task;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Fund fund(String code) {
        Fund fund = new Fund();
        fund.setCode(code);
        return fund;
    }
}
