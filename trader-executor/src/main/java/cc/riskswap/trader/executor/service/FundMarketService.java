package cc.riskswap.trader.executor.service;


import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cc.riskswap.trader.executor.dao.FundMarketDao;
import cc.riskswap.trader.executor.dao.entity.FundMarket;
import lombok.extern.slf4j.Slf4j;

/**
 * Fund market data service implementation class, responsible for fund-related business logic processing
 */
@Service
@Slf4j
public class FundMarketService {
    
    @Autowired
    private FundMarketDao fundMarketDao;

    public List<FundMarket> getData(String code, LocalDate beginDate, LocalDate endDate) {
        return fundMarketDao.getDailyData(code, beginDate, endDate);
    }
}