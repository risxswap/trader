package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.common.model.param.CorrelationParam;
import cc.riskswap.trader.admin.service.CorrelationService;
import cc.riskswap.trader.base.dao.CorrelationDao;
import cc.riskswap.trader.base.dao.entity.Correlation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CorrelationServiceTest {

    @Test
    void shouldMapEntitySymbolFieldsToDetailDtoAssetFields() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        CorrelationService correlationService = createService(correlationDao);

        Correlation entity = new Correlation();
        entity.setId(88L);
        entity.setAsset1("510300");
        entity.setAsset1Type("ETF");
        entity.setAsset2("159915");
        entity.setAsset2Type("ETF");
        entity.setPeriod("6M");
        entity.setCoefficient(BigDecimal.valueOf(0.92d));
        entity.setPValue(BigDecimal.valueOf(0.03d));
        entity.setCreatedAt(OffsetDateTime.parse("2026-04-10T08:00:00+08:00"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-04-10T08:05:00+08:00"));
        Mockito.when(correlationDao.getById(88)).thenReturn(entity);

        var detail = correlationService.detail(88L);

        Assertions.assertEquals("510300", detail.getAsset1());
        Assertions.assertEquals("ETF", detail.getAsset1Type());
        Assertions.assertEquals("159915", detail.getAsset2());
        Assertions.assertEquals("ETF", detail.getAsset2Type());
        Assertions.assertEquals("6M", detail.getPeriod());
        Assertions.assertEquals(BigDecimal.valueOf(0.92d), detail.getCoefficient());
    }

    @Test
    void shouldInsertNewCorrelationWithNewIdWithoutDeletingOldRecordOnUpdate() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        CorrelationService correlationService = createService(correlationDao);

        Correlation existing = new Correlation();
        existing.setId(100L);
        Mockito.when(correlationDao.getById(100)).thenReturn(existing);

        CorrelationParam param = new CorrelationParam();
        param.setId(100L);
        param.setAsset1("000001");
        param.setAsset1Type("ETF");
        param.setAsset2("000002");
        param.setAsset2Type("LOF");
        param.setPeriod("1Y");
        param.setCoefficient(BigDecimal.valueOf(0.81d));
        param.setPValue(BigDecimal.valueOf(0.01d));

        correlationService.update(param);

        ArgumentCaptor<Correlation> captor = ArgumentCaptor.forClass(Correlation.class);
        Mockito.verify(correlationDao).save(captor.capture());

        Correlation saved = captor.getValue();
        Assertions.assertNull(saved.getId());
        Assertions.assertEquals("000001", saved.getAsset1());
        Assertions.assertEquals("ETF", saved.getAsset1Type());
        Assertions.assertEquals("000002", saved.getAsset2());
        Assertions.assertEquals("LOF", saved.getAsset2Type());
        Assertions.assertEquals("1Y", saved.getPeriod());
        Assertions.assertEquals(BigDecimal.valueOf(0.81d), saved.getCoefficient());
        Assertions.assertEquals(BigDecimal.valueOf(0.01d), saved.getPValue());
        Assertions.assertNotNull(saved.getCreatedAt());
        Assertions.assertNotNull(saved.getUpdatedAt());
    }

    private CorrelationService createService(CorrelationDao correlationDao) throws Exception {
        CorrelationService correlationService = new CorrelationService();
        setField(correlationService, "correlationDao", correlationDao);
        return correlationService;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
