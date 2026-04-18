package cc.riskswap.trader.admin.test.util;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CorrelationTest {

    @Test
    public void correlationTest() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {2, 4, 6, 8, 10};
        PearsonsCorrelation pCorrelation = new PearsonsCorrelation();
        double correlation = pCorrelation.correlation(x, y);
        log.info("correlation: {}", correlation);
    }
}
