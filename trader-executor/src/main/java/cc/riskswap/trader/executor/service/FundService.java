package cc.riskswap.trader.executor.service;

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
import org.springframework.stereotype.Service;

import cc.riskswap.trader.executor.dao.FundDao;
import cc.riskswap.trader.executor.dao.entity.Fund;
import cc.riskswap.trader.executor.dao.entity.FundMarket;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FundService {
    
    private final FundDao fundDao;

    private final FundMarketService fundMarketService;

    public FundService(FundDao fundDao, FundMarketService fundMarketService) {
        this.fundDao = fundDao;
        this.fundMarketService = fundMarketService;
    }

    /**
     * 计算相关性
     */
    public List<Set<String>> calculateCorrelation(LocalDate startDate, LocalDate endDate, double threshold) {
        log.info("calculateCorrelation,start, threshold: {}", threshold);
        List<Fund> fundList = fundDao.listAll();
        Map<String, Map<LocalDate, Double>> fundDailyReturns = new HashMap<>();
        for (Fund fund : fundList) {
            String code = fund.getSymbol();
            List<FundMarket> markets = fundMarketService.getData(code, startDate, endDate);
            if (CollectionUtil.isEmpty(markets)) {
                continue;
            }
            Map<LocalDate, Double> data = new HashMap<>();
            for (FundMarket market : markets) {
                BigDecimal pctChg = market.getPctChg();
                data.put(market.getTime(), pctChg.doubleValue());
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
