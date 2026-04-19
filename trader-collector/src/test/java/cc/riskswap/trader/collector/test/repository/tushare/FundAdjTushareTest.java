package cc.riskswap.trader.collector.test.repository.tushare;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.common.model.query.FundAdjQuery;
import cc.riskswap.trader.collector.repository.tushare.FundAdjTushare;
import cc.riskswap.trader.base.dao.entity.FundAdj;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class FundAdjTushareTest {

    @Autowired
    private FundAdjTushare fundAdjTushare;

    @Test
    public void testList() {
        FundAdjQuery query = new FundAdjQuery();
        query.setCode("513100.SH");
        query.setStartDate(LocalDate.of(2021, 1, 1));
        query.setEndDate(LocalDate.of(2021, 12, 31));
        List<FundAdj> fundAdjs = fundAdjTushare.list(query);
        log.info("fundAdjs:{}", fundAdjs);
    }
}
