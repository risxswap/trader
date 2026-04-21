package cc.riskswap.trader.statistic.service;

import cc.riskswap.trader.base.dao.CorrelationDao;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.FundNavDao;
import cc.riskswap.trader.base.dao.entity.Correlation;
import cc.riskswap.trader.base.dao.entity.CorrelationDuplicateGroup;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundNav;
import cc.riskswap.trader.statistic.config.StatisticCorrelationProperties;
import cc.riskswap.trader.statistic.task.CorrelationTaskParams;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CorrelationService {

    private static final int CLEANUP_GROUP_PAGE_SIZE = 200;

    @Autowired
    private CorrelationDao correlationDao;

    @Autowired
    private FundDao fundDao;

    @Autowired
    private FundNavDao fundNavDao;

    @Autowired
    private StatisticCorrelationProperties correlationProperties;

    @Transactional("clickHouseTransactionManager")
    public int calculateAndSaveBatch(List<Fund> funds, String period) {
        return calculateAndSaveBatch(funds, period, CorrelationTaskParams.DEFAULT_MIN_ABS_CORRELATION);
    }

    @Transactional("clickHouseTransactionManager")
    public int calculateAndSaveBatch(List<Fund> funds, String period, double minAbsCorrelation) {
        if (funds == null || funds.isEmpty()) {
            log.info("Skip correlation batch calculation because no funds were provided. period={}, minAbsCorrelation={}",
                    period, minAbsCorrelation);
            return 0;
        }

        List<Fund> uniqueFunds = funds.stream()
                .filter(fund -> fund.getCode() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Fund::getCode, fund -> fund, (existing, ignored) -> existing, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
        if (uniqueFunds.size() < 2) {
            log.info("Skip correlation batch calculation because unique fund count is less than 2. period={}, uniqueFunds={}, minAbsCorrelation={}",
                    period, uniqueFunds.size(), minAbsCorrelation);
            return 0;
        }

        OffsetDateTime startTime = getStartTimeByPeriod(period);
        List<String> codes = uniqueFunds.stream().map(Fund::getCode).toList();
        long totalPairs = (long) uniqueFunds.size() * (uniqueFunds.size() - 1) / 2;
        int navQueryBatchSize = correlationProperties.getSafeNavQueryCodeBatchSize();
        int saveBatchSize = correlationProperties.getSafeSaveBatchSize();
        log.info("Start correlation batch calculation. period={}, inputFunds={}, uniqueFunds={}, totalPairs={}, navQueryBatchSize={}, saveBatchSize={}, minAbsCorrelation={}, startTime={}",
                period, funds.size(), uniqueFunds.size(), totalPairs, navQueryBatchSize, saveBatchSize, minAbsCorrelation, startTime);
        Map<String, Map<LocalDate, BigDecimal>> navSeriesByCode = loadNavSeriesByCode(codes, startTime);
        List<Correlation> buffer = new ArrayList<>();
        int savedCount = 0;
        long skippedEmptySeriesPairs = 0L;
        long filteredPairs = 0L;

        for (int i = 0; i < uniqueFunds.size(); i++) {
            Fund fund1 = uniqueFunds.get(i);
            Map<LocalDate, BigDecimal> series1 = navSeriesByCode.getOrDefault(fund1.getCode(), Collections.emptyMap());
            if (series1.isEmpty()) {
                continue;
            }
            for (int j = i + 1; j < uniqueFunds.size(); j++) {
                Fund fund2 = uniqueFunds.get(j);
                Map<LocalDate, BigDecimal> series2 = navSeriesByCode.getOrDefault(fund2.getCode(), Collections.emptyMap());
                if (series2.isEmpty()) {
                    skippedEmptySeriesPairs++;
                    continue;
                }
                Correlation correlation = buildCorrelationIfEligible(fund1, fund2, period, series1, series2, minAbsCorrelation);
                if (correlation == null) {
                    filteredPairs++;
                    continue;
                }
                buffer.add(correlation);
                if (buffer.size() >= saveBatchSize) {
                    savedCount += flushCorrelations(buffer);
                }
            }
        }

        savedCount += flushCorrelations(buffer);
        log.info("Finished correlation batch calculation. period={}, uniqueFunds={}, navSeriesLoaded={}, totalPairs={}, filteredPairs={}, skippedEmptySeriesPairs={}, minAbsCorrelation={}, saved={}",
                period, uniqueFunds.size(), navSeriesByCode.size(), totalPairs, filteredPairs, skippedEmptySeriesPairs, minAbsCorrelation, savedCount);
        return savedCount;
    }

    @Transactional("clickHouseTransactionManager")
    public void calculateAndSave(String asset1, String asset2, String period) {
        OffsetDateTime startTime = getStartTimeByPeriod(period);
        List<FundNav> navs1 = fundNavDao.listByCodeAndStartTime(asset1, startTime);
        List<FundNav> navs2 = fundNavDao.listByCodeAndStartTime(asset2, startTime);

        if (navs1.isEmpty() || navs2.isEmpty()) {
            log.warn("No FundNav data found for {} or {}", asset1, asset2);
            return;
        }

        Map<LocalDate, BigDecimal> map1 = toNavSeries(navs1);
        Map<LocalDate, BigDecimal> map2 = toNavSeries(navs2);
        CorrelationStats stats = calculateCorrelationStats(map1, map2);
        if (stats == null) {
            log.warn("Not enough common data points for correlation calculation between {} and {}", asset1, asset2);
            return;
        }
        saveCorrelation(asset1, asset2, period, stats.coefficient(), stats.pValue());
    }

    @Transactional("clickHouseTransactionManager")
    public int cleanupHistoricalCorrelations() {
        log.info("Start cleanup of historical correlations.");
        int deletedCount = 0;
        int offset = 0;
        List<Long> deleteBuffer = new ArrayList<>();
        int cleanupDeleteBatchSize = correlationProperties.getSafeCleanupDeleteBatchSize();

        while (true) {
            List<CorrelationDuplicateGroup> groups = correlationDao.listDuplicateGroups(CLEANUP_GROUP_PAGE_SIZE, offset);
            if (groups == null || groups.isEmpty()) {
                break;
            }

            for (CorrelationDuplicateGroup group : groups) {
                List<Long> historicalIds = correlationDao.listHistoricalIds(
                        group.getAsset1(), group.getAsset2(), group.getPeriod());
                if (historicalIds == null || historicalIds.isEmpty()) {
                    continue;
                }

                for (Long historicalId : historicalIds) {
                    deleteBuffer.add(historicalId);
                    if (deleteBuffer.size() >= cleanupDeleteBatchSize) {
                        correlationDao.deleteByIds(new ArrayList<>(deleteBuffer));
                        deletedCount += deleteBuffer.size();
                        deleteBuffer.clear();
                    }
                }
            }

            offset += CLEANUP_GROUP_PAGE_SIZE;
        }

        if (!deleteBuffer.isEmpty()) {
            correlationDao.deleteByIds(new ArrayList<>(deleteBuffer));
            deletedCount += deleteBuffer.size();
        }

        log.info("Finished cleanup of historical correlations. deleted={}", deletedCount);
        return deletedCount;
    }

    private OffsetDateTime getStartTimeByPeriod(String period) {
        OffsetDateTime now = OffsetDateTime.now();
        if (StrUtil.isBlank(period)) {
            return now.minusYears(1);
        }

        String p = period.toUpperCase();
        if (p.endsWith("D")) {
            return now.minusDays(Integer.parseInt(p.substring(0, p.length() - 1)));
        } else if (p.endsWith("M")) {
            return now.minusMonths(Integer.parseInt(p.substring(0, p.length() - 1)));
        } else if (p.endsWith("Y")) {
            return now.minusYears(Integer.parseInt(p.substring(0, p.length() - 1)));
        }

        try {
            return now.minusDays(Integer.parseInt(period));
        } catch (NumberFormatException e) {
            log.warn("Invalid period format: {}, defaulting to 1 year", period);
            return now.minusYears(1);
        }
    }

    private void saveCorrelation(String asset1, String asset2, String period, BigDecimal coefficient, BigDecimal pValue) {
        Correlation existing = correlationDao.getByUniqueKey(asset1, asset2, period);

        if (existing != null) {
            Correlation correlation = new Correlation();
            correlation.setAsset1(existing.getAsset1());
            correlation.setAsset1Type(existing.getAsset1Type());
            correlation.setAsset2(existing.getAsset2());
            correlation.setAsset2Type(existing.getAsset2Type());
            correlation.setPeriod(existing.getPeriod());
            correlation.setCoefficient(coefficient);
            correlation.setPValue(pValue);
            appendCorrelation(correlation);
            return;
        }

        Correlation correlation = new Correlation();
        correlation.setAsset1(asset1);
        correlation.setAsset2(asset2);
        correlation.setPeriod(period);
        correlation.setCoefficient(coefficient);
        correlation.setPValue(pValue);

        Fund f1 = fundDao.getByCode(asset1);
        if (f1 != null) {
            correlation.setAsset1Type(f1.getType());
        }
        Fund f2 = fundDao.getByCode(asset2);
        if (f2 != null) {
            correlation.setAsset2Type(f2.getType());
        }

        appendCorrelation(correlation);
    }

    private Map<LocalDate, BigDecimal> toNavSeries(List<FundNav> navs) {
        return navs.stream()
                .filter(nav -> nav.getTime() != null && nav.getAdjNav() != null)
                .collect(Collectors.toMap(
                        nav -> nav.getTime().toLocalDate(),
                        FundNav::getAdjNav,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Map<LocalDate, BigDecimal>> loadNavSeriesByCode(List<String> codes, OffsetDateTime startTime) {
        Map<String, Map<LocalDate, BigDecimal>> navSeriesByCode = new LinkedHashMap<>();
        int batchSize = correlationProperties.getSafeNavQueryCodeBatchSize();
        int totalBatches = (codes.size() + batchSize - 1) / batchSize;
        int totalNavRows = 0;
        for (int offset = 0; offset < codes.size(); offset += batchSize) {
            int end = Math.min(offset + batchSize, codes.size());
            List<String> batchCodes = codes.subList(offset, end);
            int batchIndex = offset / batchSize + 1;
            List<FundNav> batchNavs = fundNavDao.listByCodesAndStartTime(batchCodes, startTime);
            totalNavRows += batchNavs.size();
            log.info("Loaded NAV batch {}/{}. codes={}, navRows={}, startTime={}",
                    batchIndex, totalBatches, batchCodes.size(), batchNavs.size(), startTime);
            batchNavs.stream()
                    .filter(nav -> nav.getCode() != null && nav.getTime() != null && nav.getAdjNav() != null)
                    .forEach(nav -> navSeriesByCode
                            .computeIfAbsent(nav.getCode(), ignored -> new LinkedHashMap<>())
                            .putIfAbsent(nav.getTime().toLocalDate(), nav.getAdjNav()));
        }
        log.info("Finished loading NAV series. requestedCodes={}, loadedSeries={}, totalNavRows={}",
                codes.size(), navSeriesByCode.size(), totalNavRows);
        return navSeriesByCode;
    }

    private CorrelationStats calculateCorrelationStats(Map<LocalDate, BigDecimal> series1,
                                                       Map<LocalDate, BigDecimal> series2) {
        return calculateCorrelationStats(series1, series2, CorrelationTaskParams.DEFAULT_MIN_ABS_CORRELATION);
    }

    private CorrelationStats calculateCorrelationStats(Map<LocalDate, BigDecimal> series1,
                                                       Map<LocalDate, BigDecimal> series2,
                                                       double minAbsCorrelation) {
        List<Double> list1 = new ArrayList<>();
        List<Double> list2 = new ArrayList<>();
        series1.keySet().stream()
                .filter(series2::containsKey)
                .sorted()
                .forEach(date -> {
                    list1.add(series1.get(date).doubleValue());
                    list2.add(series2.get(date).doubleValue());
                });

        if (list1.size() < 3) {
            return null;
        }

        double[] array1 = list1.stream().mapToDouble(Double::doubleValue).toArray();
        double[] array2 = list2.stream().mapToDouble(Double::doubleValue).toArray();
        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();
        double correlationValue = pearsonsCorrelation.correlation(array1, array2);
        if (Double.isNaN(correlationValue)) {
            return null;
        }

        int n = array1.length;
        double absR = Math.abs(correlationValue);
        double pValue;
        if (absR == 1.0d) {
            pValue = 0.0d;
        } else {
            double t = correlationValue * Math.sqrt((n - 2.0d) / (1.0d - correlationValue * correlationValue));
            TDistribution tDistribution = new TDistribution(n - 2);
            pValue = 2.0d * (1.0d - tDistribution.cumulativeProbability(Math.abs(t)));
        }

        if (!(absR > minAbsCorrelation && pValue < 0.05d)) {
            return null;
        }
        return new CorrelationStats(BigDecimal.valueOf(correlationValue), BigDecimal.valueOf(pValue));
    }

    private Correlation createCorrelation(Fund fund1, Fund fund2, String period, CorrelationStats stats) {
        OffsetDateTime now = OffsetDateTime.now();
        Correlation correlation = new Correlation();
        correlation.setId(IdUtil.getSnowflakeNextId());
        correlation.setAsset1(fund1.getCode());
        correlation.setAsset1Type(fund1.getType());
        correlation.setAsset2(fund2.getCode());
        correlation.setAsset2Type(fund2.getType());
        correlation.setPeriod(period);
        correlation.setCoefficient(stats.coefficient());
        correlation.setPValue(stats.pValue());
        correlation.setCreatedAt(now);
        correlation.setUpdatedAt(now);
        return correlation;
    }

    private Correlation buildCorrelationIfEligible(Fund fund1, Fund fund2, String period,
                                                   Map<LocalDate, BigDecimal> series1,
                                                   Map<LocalDate, BigDecimal> series2,
                                                   double minAbsCorrelation) {
        CorrelationStats stats = calculateCorrelationStats(series1, series2, minAbsCorrelation);
        if (stats == null) {
            return null;
        }
        return createCorrelation(fund1, fund2, period, stats);
    }

    private int flushCorrelations(List<Correlation> buffer) {
        if (buffer.isEmpty()) {
            return 0;
        }
        List<Correlation> batch = new ArrayList<>(buffer);
        int saveBatchSize = correlationProperties.getSafeSaveBatchSize();
        log.info("Persisting correlation batch. size={}, configuredBatchSize={}", batch.size(), saveBatchSize);
        correlationDao.saveBatch(batch, saveBatchSize);
        buffer.clear();
        return batch.size();
    }

    private void appendCorrelation(Correlation correlation) {
        OffsetDateTime now = OffsetDateTime.now();
        correlation.setCreatedAt(now);
        correlation.setUpdatedAt(now);
        correlation.setId(IdUtil.getSnowflakeNextId());
        correlationDao.save(correlation);
    }

    private record CorrelationStats(BigDecimal coefficient, BigDecimal pValue) {
    }
}
