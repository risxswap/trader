package cc.riskswap.trader.collector.test.repository.tushare;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.repository.tushare.TushareManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class TushareManagerTest {

    @Autowired
    private TushareManager tushareManager;

    @Test
    public void testTushare() throws IOException {
        // 添加params参数
        Map<String, Object> params = new HashMap<>();
        params.put("list_status", "L");
        String fileds = "ts_code,name,area,industry,list_date";
        String response = tushareManager.post("stock_basic", fileds, params);
        log.info("Tushare API响应: {}", response);
    }

    @Test
    public void testEtf() {
        Map<String,Object> params = new HashMap<>();
        // arams.put("ts_code", "159543.SZ");
        // params.put("status", "L");
        params.put("market", "E");
        String fields="ts_code,name,management,custodian,fund_type,invest_type";
        String response = tushareManager.post("fund_basic",fields, params);
        log.info("Tushare API响应: {}", response);
    }

    @Test
    public void testFundDaily() {
        Map<String,Object> params = new HashMap<>();
        params.put("ts_code", "159543.SZ");
        params.put("start_date", "20251001");
        params.put("end_date", "20251020");
        String response = tushareManager.post("fund_daily",null, params);
        log.info("Tushare API响应: {}", response);
    }
}
