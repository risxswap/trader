package cc.riskswap.trader.admin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cc.riskswap.trader.admin.common.model.dto.FundAdjDto;
import cc.riskswap.trader.admin.common.model.dto.FundDto;
import cc.riskswap.trader.admin.common.model.dto.FundMarketDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.FundUpdateParam;
import cc.riskswap.trader.admin.common.model.query.FundAdjListQuery;
import cc.riskswap.trader.admin.common.model.query.FundAdjQuery;
import cc.riskswap.trader.admin.common.model.query.FundListQuery;
import cc.riskswap.trader.admin.common.model.query.FundMarketListQuery;
import cc.riskswap.trader.admin.common.model.query.FundMarketQuery;
import cc.riskswap.trader.base.dao.FundAdjDao;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.FundMarketDao;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.base.dao.entity.FundMarket;
import cc.riskswap.trader.base.dao.entity.FundAdj;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FundService {

    @Autowired
    private FundDao fundDao;

    @Autowired
    private FundMarketService fundMarketService;

    @Autowired
    private FundAdjDao fundAdjDao;

    @Autowired
    private FundMarketDao fundMarketDao;

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

    /**
     * 分页查询基金列表（包含搜索与排序）并填充最新更新时间
     */
    public PageDto<FundDto> listFunds(FundListQuery q) {
        cc.riskswap.trader.base.dao.query.FundListQuery fundListQuery = new cc.riskswap.trader.base.dao.query.FundListQuery();
        org.springframework.beans.BeanUtils.copyProperties(q, fundListQuery);
        Page<Fund> r = fundDao.pageQuery(fundListQuery);
        List<FundDto> items = r.getRecords().stream().map(f -> {
            FundDto d = new FundDto();
            d.setCode(f.getCode());
            d.setName(f.getName());
            d.setStatus(f.getStatus());
            d.setMarket(f.getMarket());
            d.setExchange(f.getExchange());
            d.setManagement(f.getManagement());
            d.setCustodian(f.getCustodian());
            d.setFundType(f.getFundType());
            d.setManagementFee(f.getMFee() != null ? f.getMFee().doubleValue() : null);
            d.setCustodianFee(f.getCFee() != null ? f.getCFee().doubleValue() : null);
            d.setListDate(f.getListDate());
            d.setFoundDate(f.getFoundDate());
            d.setUpdatedAt(f.getUpdatedAt());
            d.setCreatedAt(f.getCreatedAt());
            return d;
        }).collect(Collectors.toList());

        PageDto<FundDto> res = new PageDto<>();
        res.setItems(items);
        res.setTotal(r.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }

    /**
     * 获取基金详情
     */
    public FundDto getDetail(String code) {
        Fund f = fundDao.getByCode(code);
        if (f == null) {
            return null;
        }
        FundDto d = new FundDto();
        d.setCode(f.getCode());
        d.setName(f.getName());
        d.setStatus(f.getStatus());
        d.setMarket(f.getMarket());
        d.setExchange(f.getExchange());
        d.setManagement(f.getManagement());
        d.setCustodian(f.getCustodian());
        d.setFundType(f.getFundType());
        d.setManagementFee(f.getMFee() != null ? f.getMFee().doubleValue() : null);
        d.setCustodianFee(f.getCFee() != null ? f.getCFee().doubleValue() : null);
        d.setListDate(f.getListDate());
        d.setFoundDate(f.getFoundDate());
        d.setUpdatedAt(f.getUpdatedAt());
        d.setCreatedAt(f.getCreatedAt());
        return d;
    }

    /**
     * 更新基金基础信息
     */
    public boolean updateFundBasic(String code, FundUpdateParam p) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Fund> uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(Fund::getCode, code);
        if (p.getName() != null) uw.set(Fund::getName, p.getName());
        if (p.getStatus() != null) uw.set(Fund::getStatus, p.getStatus());
        if (p.getMarket() != null) uw.set(Fund::getMarket, p.getMarket());
        if (p.getExchange() != null) uw.set(Fund::getExchange, p.getExchange());
        return fundDao.update(uw);
    }

    /**
     * 删除基金
     */
    public boolean deleteFund(String code) {
        int n = fundDao.deleteByCode(code);
        return n > 0;
    }

    /**
     * 获取所有基金列表
     */
    public List<Fund> listAll() {
        return fundDao.listAll();
    }

    /**
     * 获取基金行情（DTO）
     */
    public List<FundMarketDto> getMarket(FundMarketQuery q) {
        List<FundMarket> data = fundMarketService.getData(q.getCode(), q.getStartDate(), q.getEndDate());
        return data.stream().map(m -> {
            FundMarketDto d = new FundMarketDto();
            d.setTime(m.getTime());
            d.setCode(m.getCode());
            d.setUpdatedAt(m.getUpdatedAt());
            d.setOpen(m.getOpen());
            d.setHigh(m.getHigh());
            d.setLow(m.getLow());
            d.setClose(m.getClose());
            d.setAmount(m.getAmount());
            d.setPctChg(m.getPctChg());
            return d;
        }).collect(Collectors.toList());
    }

    /**
     * 获取基金复权因子（DTO）
     */
    public List<FundAdjDto> getAdj(FundAdjQuery q) {
        List<FundAdj> adjs = fundAdjDao.listByCodeAndDateRange(q.getCode(), q.getStartDate(), q.getEndDate());
        return adjs.stream().map(a -> {
            FundAdjDto d = new FundAdjDto();
            d.setTime(a.getTime());
            d.setCode(a.getCode());
            d.setUpdatedAt(a.getUpdatedAt());
            d.setAdjFactor(a.getAdjFactor());
            return d;
        }).collect(Collectors.toList());
    }

    /**
     * 获取默认基金代码（按最新行情记录）
     */
    public String getDefaultFundCode() {
        return fundMarketDao.getLatestCode();
    }

    /**
     * 分页查询行情列表（代码和日期为空时返回所有数据）
     */
    public PageDto<FundMarketDto> listFundMarkets(FundMarketListQuery q) {
        cc.riskswap.trader.base.dao.query.FundMarketListQuery listQuery = new cc.riskswap.trader.base.dao.query.FundMarketListQuery();
        org.springframework.beans.BeanUtils.copyProperties(q, listQuery);
        Page<FundMarket> r = fundMarketDao.pageQuery(listQuery);
        List<FundMarketDto> items = r.getRecords().stream().map(m -> {
            FundMarketDto d = new FundMarketDto();
            d.setTime(m.getTime());
            d.setCode(m.getCode());
            d.setUpdatedAt(m.getUpdatedAt());
            d.setOpen(m.getOpen());
            d.setHigh(m.getHigh());
            d.setLow(m.getLow());
            d.setClose(m.getClose());
            d.setAmount(m.getAmount());
            d.setPctChg(m.getPctChg());
            return d;
        }).collect(Collectors.toList());
        PageDto<FundMarketDto> res = new PageDto<>();
        res.setItems(items);
        res.setTotal(r.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }

    /**
     * 分页查询复权因子（代码和日期为空时返回所有数据）
     */
    public PageDto<FundAdjDto> listFundAdjs(FundAdjListQuery q) {
        cc.riskswap.trader.base.dao.query.FundAdjListQuery listQuery = new cc.riskswap.trader.base.dao.query.FundAdjListQuery();
        org.springframework.beans.BeanUtils.copyProperties(q, listQuery);
        Page<FundAdj> r = fundAdjDao.pageQuery(listQuery);
        List<FundAdjDto> items = r.getRecords().stream().map(a -> {
            FundAdjDto d = new FundAdjDto();
            d.setTime(a.getTime());
            d.setCode(a.getCode());
            d.setUpdatedAt(a.getUpdatedAt());
            d.setAdjFactor(a.getAdjFactor());
            return d;
        }).collect(Collectors.toList());
        PageDto<FundAdjDto> res = new PageDto<>();
        res.setItems(items);
        res.setTotal(r.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }
}
