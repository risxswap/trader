package cc.riskswap.trader.collector.test.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.service.FundAdjService;

@SpringBootTest
public class FundAdjServiceTest {
    @Autowired
    private FundAdjService fundAdjService;

    @Test
    public void syncBySymbol() {
        fundAdjService.syncBySymbol();
    }
}
