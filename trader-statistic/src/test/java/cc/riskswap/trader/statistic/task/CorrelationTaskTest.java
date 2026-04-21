package cc.riskswap.trader.statistic.task;

import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.statistic.service.CorrelationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

class CorrelationTaskTest {

    @Test
    void should_call_batch_calculation_and_cleanup_once() throws Exception {
        Assertions.assertDoesNotThrow(() ->
                CorrelationService.class.getMethod("calculateAndSaveBatch", List.class, String.class));

        FundDao fundDao = Mockito.mock(FundDao.class);
        CorrelationService correlationService = Mockito.mock(CorrelationService.class);
        CorrelationTask task = createTask(fundDao, correlationService);

        Mockito.when(fundDao.listAll()).thenReturn(List.of(
                fund("510300"),
                fund("159915"),
                fund("510300"),
                fund(null)
        ));
        Mockito.when(correlationService.calculateAndSaveBatch(Mockito.anyList(), Mockito.eq("1Y"))).thenReturn(1);
        Mockito.when(correlationService.cleanupHistoricalCorrelations()).thenReturn(0);

        task.doCalculateAllCorrelations();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Fund>> fundsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(correlationService).calculateAndSaveBatch(fundsCaptor.capture(), Mockito.eq("1Y"));
        Mockito.verify(correlationService, Mockito.never())
                .calculateAndSave(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(correlationService).cleanupHistoricalCorrelations();
        Assertions.assertEquals(2, fundsCaptor.getValue().size());
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
