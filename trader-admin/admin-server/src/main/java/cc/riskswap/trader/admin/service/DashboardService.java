package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.ChartDataDto;
import cc.riskswap.trader.admin.common.model.dto.DashboardDto;
import cc.riskswap.trader.admin.common.model.dto.SystemStatusDto;
import cc.riskswap.trader.admin.dao.BrokerDao;
import cc.riskswap.trader.admin.dao.InvestmentDao;
import cc.riskswap.trader.admin.dao.InvestmentLogDao;
import cc.riskswap.trader.admin.dao.entity.Broker;
import cc.riskswap.trader.admin.dao.entity.Investment;
import cc.riskswap.trader.admin.dao.entity.InvestmentLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BrokerDao brokerDao;
    private final InvestmentDao investmentDao;
    private final InvestmentLogDao investmentLogDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Qualifier("mysqlDataSource")
    private DataSource mysqlDataSource;

    @Autowired(required = false)
    @Qualifier("clickHouseDataSource")
    private DataSource clickHouseDataSource;

    public DashboardDto getOverview() {
        DashboardDto dto = new DashboardDto();

        // 1. Broker Assets
        List<Broker> brokers = brokerDao.list();
        BigDecimal brokerTotal = brokers.stream()
                .map(Broker::getCurrentCapital)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Investment Assets & Profit
        List<Investment> investments = investmentDao.list();
        List<InvestmentLog> allLogs = investmentLogDao.list();

        // Map investmentId -> Latest Log
        Map<Integer, InvestmentLog> latestLogMap = new HashMap<>();
        for (InvestmentLog log : allLogs) {
            InvestmentLog existing = latestLogMap.get(log.getInvestmentId());
            if (existing == null || log.getRecordDate().isAfter(existing.getRecordDate())) {
                latestLogMap.put(log.getInvestmentId(), log);
            }
        }

        BigDecimal investmentTotal = BigDecimal.ZERO;
        BigDecimal todayProfit = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int activeCount = 0;
        int winCount = 0;
        int tradeCount = 0;

        // Calculate totalProfit and winRate from all logs
        for (InvestmentLog log : allLogs) {
            if (log.getProfit() != null && log.getProfit().compareTo(BigDecimal.ZERO) != 0) {
                tradeCount++;
                if (log.getProfit().compareTo(BigDecimal.ZERO) > 0) {
                    winCount++;
                }
                totalProfit = totalProfit.add(log.getProfit());
            }
        }

        for (Investment inv : investments) {
            if ("RUNNING".equals(inv.getStatus())) {
                activeCount++;
            }

            InvestmentLog latest = latestLogMap.get(inv.getId());
            BigDecimal currentVal = BigDecimal.ZERO;

            if (latest != null) {
                currentVal = latest.getAsset() != null ? latest.getAsset() : BigDecimal.ZERO;
                // Check if latest log is from today
                if (latest.getRecordDate().toLocalDate().equals(LocalDate.now())) {
                    todayProfit = todayProfit.add(latest.getProfit() != null ? latest.getProfit() : BigDecimal.ZERO);
                }
            } else {
                currentVal = inv.getBudget() != null ? inv.getBudget() : BigDecimal.ZERO;
            }
            investmentTotal = investmentTotal.add(currentVal);
        }

        dto.setTotalAsset(brokerTotal.add(investmentTotal));
        dto.setTodayChange(todayProfit);
        dto.setTotalProfit(totalProfit);
        dto.setHoldingCount(activeCount);
        dto.setRiskIndicator("Low");

        if (tradeCount > 0) {
            BigDecimal wr = new BigDecimal(winCount).divide(new BigDecimal(tradeCount), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
            dto.setWinRate(wr.toPlainString() + "%");
        } else {
            dto.setWinRate("0.00%");
        }

        // 3. Asset Trend (Last 30 Days)
        List<ChartDataDto> trend = calculateAssetTrend(allLogs, brokerTotal, investments);
        dto.setAssetTrend(trend);

        // Calculate max drawdown from trend
        dto.setMaxDrawdown(calculateMaxDrawdown(trend));

        // 4. Asset Distribution
        dto.setAssetDistribution(calculateAssetDistribution(brokers, investments, latestLogMap));

        return dto;
    }

    private String calculateMaxDrawdown(List<ChartDataDto> trend) {
        if (trend == null || trend.isEmpty()) {
            return "0.00%";
        }
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = trend.get(0).getValue();
        
        for (ChartDataDto point : trend) {
            BigDecimal val = point.getValue();
            if (val.compareTo(peak) > 0) {
                peak = val;
            } else if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drawdown = peak.subtract(val).divide(peak, 4, RoundingMode.HALF_UP);
                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }
        
        return "-" + maxDrawdown.multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public SystemStatusDto getSystemStatus() {
        SystemStatusDto status = new SystemStatusDto();
        
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            status.setRedisStatus("NORMAL");
        } catch (Exception e) {
            status.setRedisStatus("ERROR");
        }
        
        try (Connection c = mysqlDataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("SELECT 1");
            status.setMysqlStatus("NORMAL");
        } catch (Exception e) {
            status.setMysqlStatus("ERROR");
        }
        
        if (clickHouseDataSource != null) {
            try (Connection c = clickHouseDataSource.getConnection(); Statement s = c.createStatement()) {
                s.execute("SELECT 1");
                status.setClickHouseStatus("NORMAL");
            } catch (Exception e) {
                status.setClickHouseStatus("ERROR");
            }
        } else {
            status.setClickHouseStatus("DISABLED");
        }
        
        return status;
    }

    private List<ChartDataDto> calculateAssetTrend(List<InvestmentLog> logs, BigDecimal brokerTotal, List<Investment> investments) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        List<ChartDataDto> trend = new ArrayList<>();

        Map<Integer, List<InvestmentLog>> investLogsMap = logs.stream()
                .collect(Collectors.groupingBy(InvestmentLog::getInvestmentId));
        
        investLogsMap.values().forEach(list -> list.sort(Comparator.comparing(InvestmentLog::getRecordDate)));

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            BigDecimal dailyTotal = brokerTotal; 
            
            for (Investment inv : investments) {
                List<InvestmentLog> invLogs = investLogsMap.get(inv.getId());
                BigDecimal val = BigDecimal.ZERO;
                if (invLogs != null) {
                    InvestmentLog found = null;
                    for (int i = invLogs.size() - 1; i >= 0; i--) {
                        if (!invLogs.get(i).getRecordDate().toLocalDate().isAfter(date)) {
                            found = invLogs.get(i);
                            break;
                        }
                    }
                    if (found != null) {
                        val = found.getAsset() != null ? found.getAsset() : BigDecimal.ZERO;
                    }
                }
                dailyTotal = dailyTotal.add(val);
            }

            ChartDataDto point = new ChartDataDto();
            point.setLabel(date.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.setValue(dailyTotal);
            trend.add(point);
        }
        return trend;
    }

    private List<ChartDataDto> calculateAssetDistribution(List<Broker> brokers, List<Investment> investments, Map<Integer, InvestmentLog> latestLogMap) {
        List<ChartDataDto> list = new ArrayList<>();
        
        BigDecimal cashTotal = brokers.stream()
                .map(Broker::getCurrentCapital)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (cashTotal.compareTo(BigDecimal.ZERO) > 0) {
            ChartDataDto c = new ChartDataDto();
            c.setLabel("Cash (Broker)");
            c.setValue(cashTotal);
            list.add(c);
        }

        Map<String, BigDecimal> typeMap = new HashMap<>();
        for (Investment inv : investments) {
            BigDecimal val;
            if (latestLogMap.containsKey(inv.getId())) {
                val = latestLogMap.get(inv.getId()).getAsset();
            } else {
                val = inv.getBudget();
            }
            if (val == null) val = BigDecimal.ZERO;
            
            String type = inv.getInvestType();
            if (type == null) type = "Other";
            
            typeMap.put(type, typeMap.getOrDefault(type, BigDecimal.ZERO).add(val));
        }

        for (Map.Entry<String, BigDecimal> entry : typeMap.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                ChartDataDto c = new ChartDataDto();
                c.setLabel(entry.getKey());
                c.setValue(entry.getValue());
                list.add(c);
            }
        }
        
        return list;
    }
}