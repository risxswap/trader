package cc.riskswap.trader.collector.test.repository.tushare;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.repository.tushare.FundTushare;
import cc.riskswap.trader.base.dao.entity.Fund;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class FundTushareTest {

    @Autowired
    private FundTushare fundTushare;

    @Test
    public void testListFromTushare() {
        List<Fund> fundList = fundTushare.list(1, 10, "E", "L");
        for (Fund fund : fundList) {
            log.info("fund: {}", fund);
        }
    }
}
