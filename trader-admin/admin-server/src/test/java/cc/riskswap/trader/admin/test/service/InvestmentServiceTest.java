package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.cache.StrategyInfoCache;
import cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto;
import cc.riskswap.trader.admin.common.model.param.InvestmentParam;
import cc.riskswap.trader.admin.exception.Warning;
import cc.riskswap.trader.admin.service.InvestmentService;
import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class InvestmentServiceTest {

    @Autowired
    private InvestmentService investmentService;

    @Autowired
    private StrategyInfoCache strategyInfoCache;

    @BeforeEach
    public void setup() {
        List<StrategyInfoDto> list = new ArrayList<>();
        StrategyInfoDto ma = new StrategyInfoDto();
        ma.setClassName("MA_CROSS");
        ma.setName("双均线趋势跟踪");
        cn.hutool.json.JSONArray req = new cn.hutool.json.JSONArray();
        req.add("shortWindow");
        req.add("longWindow");
        req.add("maxPosition");
        JSONObject props = new JSONObject()
                .set("shortWindow", new JSONObject().set("type", "integer").set("minimum", 1).set("maximum", 200))
                .set("longWindow", new JSONObject().set("type", "integer").set("minimum", 2).set("maximum", 400))
                .set("maxPosition", new JSONObject().set("type", "number").set("minimum", 0).set("maximum", 1));
        ma.setConfigSchame(new JSONObject()
                .set("$schema", "https://json-schema.org/draft/2020-12/schema")
                .set("type", "object")
                .set("required", req)
                .set("properties", props)
                .toString());
        list.add(ma);
        strategyInfoCache.cacheStrategies(list);
    }

    @Test
    public void testAddInvalidStrategyShouldFail() {
        InvestmentParam p = new InvestmentParam();
        p.setName("A");
        p.setBrokerId(1);
        p.setStrategy("UNKNOWN");
        p.setStrategyConfig("{}");
        Assertions.assertThrows(Warning.class, () -> investmentService.add(p));
    }

    @Test
    public void testAddMissingConfigShouldFail() {
        InvestmentParam p = new InvestmentParam();
        p.setName("B");
        p.setBrokerId(1);
        p.setStrategy("MA_CROSS");
        p.setStrategyConfig(null);
        Assertions.assertThrows(Warning.class, () -> investmentService.add(p));
    }

    @Test
    public void testAddInvalidConfigValueOutOfRangeShouldFail() {
        InvestmentParam p = new InvestmentParam();
        p.setName("C");
        p.setBrokerId(1);
        p.setStrategy("MA_CROSS");
        p.setStrategyConfig(new JSONObject()
                .set("shortWindow", 20)
                .set("longWindow", 60)
                .set("maxPosition", 2.0)
                .toString());
        Assertions.assertThrows(Warning.class, () -> investmentService.add(p));
    }

    @Test
    public void testUpdateInvalidShouldFail() {
        InvestmentParam p = new InvestmentParam();
        p.setId(999);
        p.setName("D");
        p.setBrokerId(1);
        p.setStrategy("MA_CROSS");
        p.setStrategyConfig(new JSONObject()
                .set("shortWindow", 20)
                .set("longWindow", 60)
                .set("maxPosition", 2.0)
                .toString());
        Assertions.assertThrows(Warning.class, () -> investmentService.update(p));
    }
}
