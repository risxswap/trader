package cc.riskswap.trader.statistic.service;

import cc.riskswap.trader.base.dao.CorrelationDao;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.FundNavDao;
import cc.riskswap.trader.base.dao.entity.Correlation;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundNav;
import cc.riskswap.trader.statistic.config.StatisticCorrelationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class CorrelationServiceTest {

    @Test
    void should_extract_shared_correlation_helpers_for_single_and_batch_flows() {
        Assertions.assertNotNull(findMethod("toNavSeries", List.class));
        Assertions.assertNotNull(findMethod("calculateCorrelationStats", Map.class, Map.class));
        Assertions.assertNotNull(findMethod("calculateCorrelationStats", Map.class, Map.class, double.class));
        Assertions.assertNotNull(findMethod("createCorrelation", 4));
    }

    @Test
    void should_wrap_batch_calculation_in_transaction() throws Exception {
        Method method = CorrelationService.class.getMethod("calculateAndSaveBatch", List.class, String.class, double.class);

        Transactional transactional = method.getAnnotation(Transactional.class);
        Assertions.assertNotNull(transactional);
        Assertions.assertEquals("clickHouseTransactionManager", transactional.value());
    }

    @Test
    void should_wrap_cleanup_in_clickhouse_transaction() throws Exception {
        Method method = CorrelationService.class.getMethod("cleanupHistoricalCorrelations");

        Transactional transactional = method.getAnnotation(Transactional.class);
        Assertions.assertNotNull(transactional);
        Assertions.assertEquals("clickHouseTransactionManager", transactional.value());
    }

    @Test
    void should_batch_load_navs_and_persist_only_matching_correlations() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        FundDao fundDao = Mockito.mock(FundDao.class);
        FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
        CorrelationService service = createService(correlationDao, fundDao, fundNavDao);

        List<Fund> funds = List.of(
                createFund("A", "ETF"),
                createFund("B", "LOF"),
                createFund("C", "QDII")
        );
        List<FundNav> navs = List.of(
                nav("A", "2026-01-01T00:00:00Z", 1.0d),
                nav("A", "2026-01-02T00:00:00Z", 2.0d),
                nav("A", "2026-01-03T00:00:00Z", 3.0d),
                nav("A", "2026-01-04T00:00:00Z", 4.0d),
                nav("A", "2026-01-05T00:00:00Z", 5.0d),
                nav("B", "2026-01-01T00:00:00Z", 2.0d),
                nav("B", "2026-01-02T00:00:00Z", 4.0d),
                nav("B", "2026-01-03T00:00:00Z", 6.0d),
                nav("B", "2026-01-04T00:00:00Z", 8.0d),
                nav("B", "2026-01-05T00:00:00Z", 10.0d),
                nav("C", "2026-01-01T00:00:00Z", 7.0d),
                nav("C", "2026-01-02T00:00:00Z", 8.0d)
        );

        Mockito.when(fundNavDao.listByCodesAndStartTime(Mockito.anyList(), Mockito.any())).thenReturn(navs);
        Mockito.when(correlationDao.saveBatch(Mockito.anyCollection(), Mockito.anyInt())).thenReturn(true);

        int savedCount = invokeBatch(service, funds, "1Y");

        Assertions.assertEquals(1, savedCount);
        Mockito.verify(fundNavDao).listByCodesAndStartTime(Mockito.eq(List.of("A", "B", "C")), Mockito.any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Correlation>> captor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(correlationDao).saveBatch(captor.capture(), Mockito.anyInt());
        Collection<Correlation> saved = captor.getValue();
        Assertions.assertEquals(1, saved.size());

        Correlation correlation = saved.iterator().next();
        Assertions.assertEquals("A", correlation.getAsset1());
        Assertions.assertEquals("ETF", correlation.getAsset1Type());
        Assertions.assertEquals("B", correlation.getAsset2());
        Assertions.assertEquals("LOF", correlation.getAsset2Type());
        Assertions.assertEquals("1Y", correlation.getPeriod());
        Assertions.assertTrue(correlation.getCoefficient().abs().compareTo(BigDecimal.valueOf(0.5d)) > 0);
    }

    @Test
    void should_flush_remaining_correlations_when_buffer_is_not_full() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        FundDao fundDao = Mockito.mock(FundDao.class);
        FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
        CorrelationService service = createService(correlationDao, fundDao, fundNavDao);

        List<Fund> funds = List.of(
                createFund("A", "ETF"),
                createFund("B", "LOF")
        );
        List<FundNav> navs = List.of(
                nav("A", "2026-01-01T00:00:00Z", 1.0d),
                nav("A", "2026-01-02T00:00:00Z", 2.0d),
                nav("A", "2026-01-03T00:00:00Z", 3.0d),
                nav("A", "2026-01-04T00:00:00Z", 4.0d),
                nav("A", "2026-01-05T00:00:00Z", 5.0d),
                nav("B", "2026-01-01T00:00:00Z", 2.0d),
                nav("B", "2026-01-02T00:00:00Z", 4.0d),
                nav("B", "2026-01-03T00:00:00Z", 6.0d),
                nav("B", "2026-01-04T00:00:00Z", 8.0d),
                nav("B", "2026-01-05T00:00:00Z", 10.0d)
        );

        Mockito.when(fundNavDao.listByCodesAndStartTime(Mockito.anyList(), Mockito.any())).thenReturn(navs);
        Mockito.when(correlationDao.saveBatch(Mockito.anyCollection(), Mockito.anyInt())).thenReturn(true);

        int savedCount = invokeBatch(service, funds, "1Y");

        Assertions.assertEquals(1, savedCount);
        Mockito.verify(correlationDao, Mockito.times(1)).saveBatch(Mockito.anyCollection(), Mockito.anyInt());
    }

    @Test
    void should_split_nav_loading_into_multiple_queries_when_fund_count_is_large() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        FundDao fundDao = Mockito.mock(FundDao.class);
        FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
        StatisticCorrelationProperties properties = new StatisticCorrelationProperties();
        properties.setNavQueryCodeBatchSize(200);
        CorrelationService service = createService(correlationDao, fundDao, fundNavDao, properties);

        List<Fund> funds = new ArrayList<>();
        for (int i = 0; i < 401; i++) {
            funds.add(createFund(String.format("F%03d", i), "ETF"));
        }

        Mockito.when(fundNavDao.listByCodesAndStartTime(Mockito.anyList(), Mockito.any()))
                .thenReturn(List.of());

        int savedCount = invokeBatch(service, funds, "1Y");

        Assertions.assertEquals(0, savedCount);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(fundNavDao, Mockito.times(3)).listByCodesAndStartTime(captor.capture(), Mockito.any());
        List<List<String>> batches = captor.getAllValues();
        Assertions.assertEquals(List.of(200, 200, 1),
                batches.stream().map(List::size).toList());
    }

    @Test
    void should_split_nav_loading_by_configured_batch_size() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        FundDao fundDao = Mockito.mock(FundDao.class);
        FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
        StatisticCorrelationProperties properties = new StatisticCorrelationProperties();
        properties.setNavQueryCodeBatchSize(3);
        CorrelationService service = createService(correlationDao, fundDao, fundNavDao, properties);

        List<Fund> funds = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            funds.add(createFund(String.format("F%03d", i), "ETF"));
        }

        Mockito.when(fundNavDao.listByCodesAndStartTime(Mockito.anyList(), Mockito.any()))
                .thenReturn(List.of());

        int savedCount = invokeBatch(service, funds, "1Y");

        Assertions.assertEquals(0, savedCount);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(fundNavDao, Mockito.times(3)).listByCodesAndStartTime(captor.capture(), Mockito.any());
        List<List<String>> batches = captor.getAllValues();
        Assertions.assertEquals(List.of(3, 3, 1),
                batches.stream().map(List::size).toList());
    }

    @Test
    void should_use_configured_save_batch_size_when_persisting_correlations() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        FundDao fundDao = Mockito.mock(FundDao.class);
        FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
        StatisticCorrelationProperties properties = new StatisticCorrelationProperties();
        properties.setSaveBatchSize(1);
        CorrelationService service = createService(correlationDao, fundDao, fundNavDao, properties);

        List<Fund> funds = List.of(
                createFund("A", "ETF"),
                createFund("B", "LOF")
        );
        List<FundNav> navs = List.of(
                nav("A", "2026-01-01T00:00:00Z", 1.0d),
                nav("A", "2026-01-02T00:00:00Z", 2.0d),
                nav("A", "2026-01-03T00:00:00Z", 3.0d),
                nav("A", "2026-01-04T00:00:00Z", 4.0d),
                nav("A", "2026-01-05T00:00:00Z", 5.0d),
                nav("B", "2026-01-01T00:00:00Z", 2.0d),
                nav("B", "2026-01-02T00:00:00Z", 4.0d),
                nav("B", "2026-01-03T00:00:00Z", 6.0d),
                nav("B", "2026-01-04T00:00:00Z", 8.0d),
                nav("B", "2026-01-05T00:00:00Z", 10.0d)
        );

        Mockito.when(fundNavDao.listByCodesAndStartTime(Mockito.anyList(), Mockito.any())).thenReturn(navs);
        Mockito.when(correlationDao.saveBatch(Mockito.anyCollection(), Mockito.anyInt())).thenReturn(true);

        int savedCount = invokeBatch(service, funds, "1Y");

        Assertions.assertEquals(1, savedCount);
        Mockito.verify(correlationDao).saveBatch(Mockito.anyCollection(), Mockito.eq(1));
    }

    @Test
    void should_filter_correlations_by_requested_min_abs_correlation() throws Exception {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        FundDao fundDao = Mockito.mock(FundDao.class);
        FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
        CorrelationService service = createService(correlationDao, fundDao, fundNavDao);

        List<Fund> funds = List.of(
                createFund("A", "ETF"),
                createFund("B", "LOF")
        );
        List<FundNav> navs = List.of(
                nav("A", "2026-01-01T00:00:00Z", 1.0d),
                nav("A", "2026-01-02T00:00:00Z", 2.0d),
                nav("A", "2026-01-03T00:00:00Z", 3.0d),
                nav("A", "2026-01-04T00:00:00Z", 4.0d),
                nav("A", "2026-01-05T00:00:00Z", 5.0d),
                nav("A", "2026-01-06T00:00:00Z", 6.0d),
                nav("B", "2026-01-01T00:00:00Z", 2.0d),
                nav("B", "2026-01-02T00:00:00Z", 1.0d),
                nav("B", "2026-01-03T00:00:00Z", 4.0d),
                nav("B", "2026-01-04T00:00:00Z", 3.0d),
                nav("B", "2026-01-05T00:00:00Z", 6.0d),
                nav("B", "2026-01-06T00:00:00Z", 5.0d)
        );

        Mockito.when(fundNavDao.listByCodesAndStartTime(Mockito.anyList(), Mockito.any())).thenReturn(navs);
        Mockito.when(correlationDao.saveBatch(Mockito.anyCollection(), Mockito.anyInt())).thenReturn(true);

        int savedWithRelaxedThreshold = invokeBatch(service, funds, "1Y", 0.5d);
        int savedWithStrictThreshold = invokeBatch(service, funds, "1Y", 0.9d);

        Assertions.assertEquals(1, savedWithRelaxedThreshold);
        Assertions.assertEquals(0, savedWithStrictThreshold);
        Mockito.verify(correlationDao, Mockito.times(1)).saveBatch(Mockito.anyCollection(), Mockito.anyInt());
    }

    @Test
    void should_use_configured_cleanup_delete_batch_size_when_cleaning_historical_correlations() {
        CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
        FundDao fundDao = Mockito.mock(FundDao.class);
        FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
        StatisticCorrelationProperties properties = new StatisticCorrelationProperties();
        properties.setCleanupDeleteBatchSize(2);
        CorrelationService service = Assertions.assertDoesNotThrow(
                () -> createService(correlationDao, fundDao, fundNavDao, properties));

        Mockito.when(correlationDao.listDuplicateGroups(200, 0))
                .thenReturn(List.of(group("A", "B", "1Y")));
        Mockito.when(correlationDao.listDuplicateGroups(200, 200))
                .thenReturn(List.of());
        Mockito.when(correlationDao.listHistoricalIds("A", "B", "1Y"))
                .thenReturn(List.of(1L, 2L, 3L));

        int deleted = service.cleanupHistoricalCorrelations();

        Assertions.assertEquals(3, deleted);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(correlationDao, Mockito.times(2)).deleteByIds(captor.capture());
        Assertions.assertEquals(List.of(List.of(1L, 2L), List.of(3L)), captor.getAllValues());
    }

    private int invokeBatch(CorrelationService service, List<Fund> funds, String period) throws Exception {
        Method method = CorrelationService.class.getMethod("calculateAndSaveBatch", List.class, String.class);
        Object result = method.invoke(service, funds, period);
        return (Integer) result;
    }

    private int invokeBatch(CorrelationService service, List<Fund> funds, String period, double minAbsCorrelation)
            throws Exception {
        Method method = CorrelationService.class.getMethod("calculateAndSaveBatch", List.class, String.class, double.class);
        Object result = method.invoke(service, funds, period, minAbsCorrelation);
        return (Integer) result;
    }

    private CorrelationService createService(CorrelationDao correlationDao, FundDao fundDao, FundNavDao fundNavDao)
            throws Exception {
        return createService(correlationDao, fundDao, fundNavDao, new StatisticCorrelationProperties());
    }

    private CorrelationService createService(CorrelationDao correlationDao,
                                             FundDao fundDao,
                                             FundNavDao fundNavDao,
                                             StatisticCorrelationProperties properties)
            throws Exception {
        CorrelationService service = new CorrelationService();
        setField(service, "correlationDao", correlationDao);
        setField(service, "fundDao", fundDao);
        setField(service, "fundNavDao", fundNavDao);
        setField(service, "correlationProperties", properties);
        return service;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Fund createFund(String code, String type) {
        Fund fund = new Fund();
        fund.setCode(code);
        fund.setType(type);
        return fund;
    }

    private cc.riskswap.trader.base.dao.entity.CorrelationDuplicateGroup group(String asset1, String asset2, String period) {
        cc.riskswap.trader.base.dao.entity.CorrelationDuplicateGroup group =
                new cc.riskswap.trader.base.dao.entity.CorrelationDuplicateGroup();
        group.setAsset1(asset1);
        group.setAsset2(asset2);
        group.setPeriod(period);
        return group;
    }

    private FundNav nav(String code, String time, double adjNav) {
        FundNav fundNav = new FundNav();
        fundNav.setCode(code);
        fundNav.setTime(OffsetDateTime.parse(time));
        fundNav.setAdjNav(BigDecimal.valueOf(adjNav));
        return fundNav;
    }

    private Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            Method method = CorrelationService.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Method findMethod(String methodName, int parameterCount) {
        for (Method method : CorrelationService.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
