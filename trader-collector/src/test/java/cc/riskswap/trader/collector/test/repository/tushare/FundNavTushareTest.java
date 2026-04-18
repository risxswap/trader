package cc.riskswap.trader.collector.test.repository.tushare;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.common.model.query.FundNavQuery;
import cc.riskswap.trader.collector.repository.entity.FundNav;
import cc.riskswap.trader.collector.repository.tushare.FundNavTushare;

import java.util.List;


@SpringBootTest
@Slf4j
public class FundNavTushareTest {

    @Autowired
    private FundNavTushare fundNavTushare;

    @Test
    public void testList() {

        // 2. 准备查询参数
        FundNavQuery query = new FundNavQuery();
        query.setCode("165509.SZ");
        // 3. 调用被测试方法
        List<FundNav> result = fundNavTushare.list(query);
        for(FundNav fundNav : result) {
            log.info("fundNav: {}", fundNav);
        }
    }
}
