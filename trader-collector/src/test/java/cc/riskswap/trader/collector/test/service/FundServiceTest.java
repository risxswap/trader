package cc.riskswap.trader.collector.test.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.service.FundService;

@SpringBootTest
public class FundServiceTest {

    @Autowired
    private FundService fundService;

    @Test
    public void testSyncFund() {
        fundService.syncFund();
    }
}
