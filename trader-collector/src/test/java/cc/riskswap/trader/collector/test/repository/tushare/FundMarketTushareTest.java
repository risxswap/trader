package cc.riskswap.trader.collector.test.repository.tushare;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.common.model.query.FundMarketQuery;
import cc.riskswap.trader.collector.repository.entity.FundMarket;
import cc.riskswap.trader.collector.repository.tushare.FundMarketTushare;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class FundMarketTushareTest {

    @Autowired
    private FundMarketTushare fundMarketTushare;

    @Test
    public void listTest() {
        FundMarketQuery query = new FundMarketQuery();
        // query.setCode("551300.SH");
        // query.setStartDate(LocalDate.of(2025, 10, 10));
        // query.setEndDate(LocalDate.of(2025, 10, 22));
        query.setTradeDate(LocalDate.of(2025, 10, 7));
        List<FundMarket> fundMarketList = fundMarketTushare.list(query);
        for (FundMarket fundMarket : fundMarketList) {
            log.info("fundMarket: {}", fundMarket);
        }
    }
}
