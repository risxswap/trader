package cc.riskswap.trader.admin.service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cc.riskswap.trader.admin.common.enums.AdjTypeEnum;
import cc.riskswap.trader.base.dao.FundAdjDao;
import cc.riskswap.trader.base.dao.FundMarketDao;
import cc.riskswap.trader.base.dao.entity.FundAdj;
import cc.riskswap.trader.base.dao.entity.FundMarket;
import lombok.extern.slf4j.Slf4j;

/**
 * Fund market data service implementation class, responsible for fund-related business logic processing
 */
@Service
@Slf4j
public class FundMarketService {
    
    @Autowired
    private FundMarketDao fundMarketDao;

    @Autowired
    private FundAdjDao fundAdjDao;

    public List<FundMarket> getData(String code, LocalDate beginDate, LocalDate endDate) {
        return fundMarketDao.getDailyData(code, beginDate, endDate);
    }

    public List<FundMarket> getAdjData(String code, LocalDate beginDate, LocalDate endDate, AdjTypeEnum type) {
        List<FundMarket> markets = fundMarketDao.getDailyData(code, beginDate, endDate);
        List<FundAdj> adjs = fundAdjDao.listByCodeAndDateRange(code, beginDate, endDate);
        if (markets == null || markets.isEmpty()) {
            return new ArrayList<>();
        }
        Map<LocalDate, BigDecimal> adjByDate = new HashMap<>();
        for (FundAdj adj : adjs) {
            if (adj.getTime() != null && adj.getAdjFactor() != null) {
                adjByDate.put(adj.getTime().toLocalDate(), BigDecimal.valueOf(adj.getAdjFactor()));
            }
        }
        BigDecimal lastKnownFactor = new BigDecimal(1L);
        BigDecimal latestFactor = new BigDecimal(1L);
        if (!adjs.isEmpty()) {
            latestFactor = BigDecimal.valueOf(adjs.get(adjs.size()-1).getAdjFactor());
        }
        for (FundMarket m : markets) {
            LocalDate d = m.getTime().toLocalDate();
            BigDecimal factor = adjByDate.getOrDefault(d, lastKnownFactor);
            if (factor != null) {
                lastKnownFactor = factor;
            }
            m.setClose(calAdjPrice(m.getClose(), lastKnownFactor, latestFactor, type));
            m.setOpen(calAdjPrice(m.getOpen(), lastKnownFactor, latestFactor, type));
            m.setHigh(calAdjPrice(m.getHigh(), lastKnownFactor, latestFactor, type));
            m.setLow(calAdjPrice(m.getLow(), lastKnownFactor, latestFactor, type));
            m.setAmount(calAdjPrice(m.getAmount(), lastKnownFactor, latestFactor, type));
        }
        return markets;
    }

    private BigDecimal calAdjPrice(BigDecimal price, BigDecimal factor, BigDecimal latestFactor, AdjTypeEnum type) {
        if (price == null) {
            return price;
        }
        if (factor == null) {
            return price;
        }
        BigDecimal result = price;
        if (AdjTypeEnum.FRONT.equals(type)) {
            result = price.multiply(factor).divide(latestFactor);
        } else {
            result = price.multiply(factor);
        }
        return result;
    }
}
