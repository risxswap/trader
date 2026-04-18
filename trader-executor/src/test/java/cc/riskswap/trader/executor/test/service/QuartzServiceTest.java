package cc.riskswap.trader.executor.test.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.executor.Application;
import cc.riskswap.trader.executor.service.InvestmentService;

@SpringBootTest(classes = Application.class)
public class QuartzServiceTest {

    @Autowired
    private InvestmentService quartzService;

    @Test
    public void testAddJob() {
    }
}
