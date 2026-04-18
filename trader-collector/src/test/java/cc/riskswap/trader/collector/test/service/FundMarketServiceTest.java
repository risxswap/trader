package cc.riskswap.trader.collector.test.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.service.FundMarketService;

@SpringBootTest
public class FundMarketServiceTest {

    @Autowired
    private FundMarketService fundMarketService;
    
    @Test
    public void syncTest() {
        fundMarketService.syncBySymbol("161227.SZ");
    }
}
