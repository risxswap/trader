package cc.riskswap.trader.collector.test.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.service.FundNavService;
import cc.riskswap.trader.base.task.TraderTaskContext;

@SpringBootTest
public class FundNavServiceTest {

    @Autowired
    private FundNavService fundNavService;

    @Test
    public void testSyncFundNav() {
        fundNavService.syncBySymbol();
    }

    @Test
    public void testSyncByNavDate() {
        fundNavService.syncFundNav(new TraderTaskContext());
    }
}
