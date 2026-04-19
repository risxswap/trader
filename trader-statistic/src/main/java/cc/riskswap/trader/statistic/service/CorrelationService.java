package cc.riskswap.trader.statistic.service;

import cc.riskswap.trader.base.dao.CorrelationDao;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.FundNavDao;
import cc.riskswap.trader.base.dao.entity.Correlation;
import cc.riskswap.trader.base.dao.entity.CorrelationDuplicateGroup;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundNav;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CorrelationService {

    private static final int CLEANUP_GROUP_PAGE_SIZE = 200;
    private static final int CLEANUP_DELETE_BATCH_SIZE = 200;

    @Autowired
    private CorrelationDao correlationDao;

    @Autowired
    private FundDao fundDao;

    @Autowired
    private FundNavDao fundNavDao;

    public void calculateAndSave(String asset1, String asset2, String period) {
        OffsetDateTime startTime = getStartTimeByPeriod(period);
        List<FundNav> navs1 = fundNavDao.listByCodeAndStartTime(asset1, startTime);
        List<FundNav> navs2 = fundNavDao.listByCodeAndStartTime(asset2, startTime);

        if (navs1.isEmpty() || navs2.isEmpty()) {
            log.warn("No FundNav data found for {} or {}", asset1, asset2);
            return;
        }

        Map<LocalDate, BigDecimal> map1 = navs1.stream()
                .filter(n -> n.getAdjNav() != null)
                .collect(Collectors.toMap(
                        n -> n.getTime().toLocalDate(),
                        FundNav::getAdjNav,
                        (v1, v2) -> v1
                ));

        Map<LocalDate, BigDecimal> map2 = navs2.stream()
                .filter(n -> n.getAdjNav() != null)
                .collect(Collectors.toMap(
                        n -> n.getTime().toLocalDate(),
                        FundNav::getAdjNav,
                        (v1, v2) -> v1
                ));

        List<Double> list1 = new ArrayList<>();
        List<Double> list2 = new ArrayList<>();
        map1.keySet().stream()
                .filter(map2::containsKey)
                .sorted()
                .forEach(date -> {
                    list1.add(map1.get(date).doubleValue());
                    list2.add(map2.get(date).doubleValue());
                });

        if (list1.size() < 3) {
            log.warn("Not enough common data points for correlation calculation between {} and {}", asset1, asset2);
            return;
        }

        double[] array1 = list1.stream().mapToDouble(Double::doubleValue).toArray();
        double[] array2 = list2.stream().mapToDouble(Double::doubleValue).toArray();
        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();
        double correlationValue = pearsonsCorrelation.correlation(array1, array2);
        if (Double.isNaN(correlationValue)) {
            return;
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

        if (!(absR > 0.5d && pValue < 0.05d)) {
            return;
        }

        saveCorrelation(asset1, asset2, period, BigDecimal.valueOf(correlationValue), BigDecimal.valueOf(pValue));
    }

    public int cleanupHistoricalCorrelations() {
        int deletedCount = 0;
        int offset = 0;
        List<Long> deleteBuffer = new ArrayList<>();

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
                    if (deleteBuffer.size() >= CLEANUP_DELETE_BATCH_SIZE) {
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

    private void appendCorrelation(Correlation correlation) {
        OffsetDateTime now = OffsetDateTime.now();
        correlation.setCreatedAt(now);
        correlation.setUpdatedAt(now);
        correlation.setId(IdUtil.getSnowflakeNextId());
        correlationDao.save(correlation);
    }
}
