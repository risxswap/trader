package cc.riskswap.trader.collector.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cc.riskswap.trader.collector.common.util.TaskContentContext;
import cc.riskswap.trader.collector.repository.tushare.FundTushare;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundMarket;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FundService {

    @Autowired
    private FundDao fundDao;

    @Autowired
    private FundTushare fundTushare;

    @Autowired
    private FundNavService fundNavService;

    @Autowired
    private FundMarketService fundMarketService;

    /**
     * 同步基金信息
     */
    public void syncFund(TraderTaskContext context) {
        TaskContentContext.addAttribute("同步市场", "E/L, O/L");
        SyncResult first = syncFund("E", "L");
        SyncResult second = syncFund("O", "L");
        long synced = (long) first.inserted + first.updated + second.inserted + second.updated;
        long failed = (long) first.errors + second.errors;
        context.report().addSynced(synced);
        context.report().addFailed(failed);
        context.report().setMessage(String.format("基金基础信息同步完成 synced=%d failed=%d", synced, failed));
        context.report().putErrorDetail("fundBase", Map.of(
                "pulled", first.total + second.total,
                "inserted", first.inserted + second.inserted,
                "updated", first.updated + second.updated,
                "errors", first.errors + second.errors
        ));
        fundNavService.syncFundNav(context);
        TaskContentContext.addDetail("执行链路", "基金基础信息同步完成后触发基金净值同步");
    }

    private SyncResult syncFund(String market, String status) {
        log.info("syncFund,start, market: {}, status: {}", market, status);
        Integer pageNo = 1;
        Integer pageSize = 500;
        Integer total = 0;
        int inserted = 0;
        int updated = 0;
        int errors = 0;
        int pageCount = 0;
        while (true) {
            List<Fund> fundList = fundTushare.list(pageNo, pageSize, market, status);
            if (fundList == null || fundList.isEmpty()) {
                break;
            }
            pageCount++;
            total += fundList.size();
            Set<String> fondCodes = new HashSet<>();
            for (Fund fund : fundList) {
                Fund existsFund = fundDao.getByCode(fund.getCode());
                try {
                    if (existsFund == null) {
                        fundDao.save(fund);
                        inserted++;
                    } else {
                        BeanUtils.copyProperties(fund, existsFund, "id");
                        fundDao.updateById(existsFund);
                        updated++;
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("syncFund,error, fund: {}", fund, e);
                    TaskContentContext.addError(String.format("基金 %s 写入失败: %s", fund.getCode(), e.getMessage()));
                }
                fondCodes.add(fund.getCode());
            }
            if (fundList.isEmpty()) {
                break;
            }
            log.info("syncFund,funds: {}", fondCodes);
            pageNo++;
        }
        TaskContentContext.addMetric("基金分页数", pageCount);
        TaskContentContext.addMetric("拉取基金数", total.longValue());
        TaskContentContext.addMetric("新增基金数", inserted);
        TaskContentContext.addMetric("更新基金数", updated);
        TaskContentContext.addMetric("基金处理异常数", errors);
        TaskContentContext.addDetail("基金基础信息",
                String.format("market=%s,status=%s,分页=%d,拉取=%d,新增=%d,更新=%d,异常=%d",
                        market, status, pageCount, total, inserted, updated, errors));
        log.info("syncFund,end, market: {}, status: {}, total: {}", market, status, total);
        return new SyncResult(total, inserted, updated, errors);
    }

    private record SyncResult(int total, int inserted, int updated, int errors) {
    }

    /**
     * 计算相关性
     */
    public List<Set<String>> calculateCorrelation(LocalDate startDate, LocalDate endDate, double threshold) {
        log.info("calculateCorrelation,start, threshold: {}", threshold);
        List<Fund> fundList = fundDao.listAll();
        Map<String, Map<LocalDate, Double>> fundDailyReturns = new HashMap<>();
        for (Fund fund : fundList) {
            String code = fund.getCode();
            List<FundMarket> markets = fundMarketService.getData(code, startDate, endDate);
            if (CollectionUtil.isEmpty(markets)) {
                continue;
            }
            Map<LocalDate, Double> data = new HashMap<>();
            for (FundMarket market : markets) {
                BigDecimal pctChg = market.getPctChg();
                data.put(market.getTime().toLocalDate(), pctChg.doubleValue());
            }
            fundDailyReturns.put(code, data);
        }

        // 使用 Apache Commons Math 计算两两相关性
        PearsonsCorrelation pc = new PearsonsCorrelation();

        // 使用 JGraphT 构建无向图
        Graph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

        List<String> codes = new ArrayList<>(fundDailyReturns.keySet());
        // 顶点
        for (String code : codes) {
            graph.addVertex(code);
        }
        // 边（相关性达到阈值）
        for (int i = 0; i < codes.size(); i++) {
            String a = codes.get(i);
            Map<LocalDate, Double> aSeries = fundDailyReturns.get(a);
            for (int j = i + 1; j < codes.size(); j++) {
                String b = codes.get(j);
                Map<LocalDate, Double> bSeries = fundDailyReturns.get(b);
                Set<LocalDate> common = new HashSet<>(aSeries.keySet());
                common.retainAll(bSeries.keySet());
                if (common.size() < 30) {
                    continue;
                }
                List<LocalDate> alignedDates = common.stream().sorted().collect(Collectors.toList());
                double[] x = new double[alignedDates.size()];
                double[] y = new double[alignedDates.size()];
                for (int k = 0; k < alignedDates.size(); k++) {
                    LocalDate d = alignedDates.get(k);
                    x[k] = aSeries.get(d);
                    y[k] = bSeries.get(d);
                }

                double corr = pc.correlation(x, y);
                if (Double.isFinite(corr) && Math.abs(corr) >= threshold) {
                    graph.addEdge(a, b);
                }
            }
        }

        // 使用 JGraphT 的连通分量分析
        ConnectivityInspector<String, DefaultEdge> inspector = new ConnectivityInspector<>(graph);
        List<Set<String>> result = inspector.connectedSets();
        return result;
    }
}
