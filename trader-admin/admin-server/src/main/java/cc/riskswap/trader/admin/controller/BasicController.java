package cc.riskswap.trader.admin.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto;
import cc.riskswap.trader.admin.common.model.dto.StrategiesDto;
import cc.riskswap.trader.base.dao.entity.Fund;
import cc.riskswap.trader.admin.service.FundService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/basic")
public class BasicController {

    @Autowired
    private FundService fundService;

    @Autowired
    private cc.riskswap.trader.admin.cache.StrategyInfoCache strategyInfoCache;

    @GetMapping("/strategies")
    public ResData<StrategiesDto> getStrategies() {
        List<StrategyInfoDto> list = strategyInfoCache.getAllStrategies();
        if (list == null || list.isEmpty()) {
            list = buildDefaultStrategies();
            strategyInfoCache.cacheStrategies(list);
        }
        StrategiesDto dto = new StrategiesDto();
        dto.setItems(list);
        dto.setVersion("v1");
        dto.setLastUpdated(OffsetDateTime.now().toString());
        return ResData.success(dto);
    }

    @GetMapping("/strategies/schema")
    public ResData<String> getStrategySchema(@RequestParam String name) {
        if (StrUtil.isBlank(name)) {
            return ResData.badRequest("name不能为空");
        }
        StrategyInfoDto info = strategyInfoCache.getStrategy(name);
        if (info == null || StrUtil.isBlank(info.getConfigSchame())) {
            return ResData.notFound("策略不存在或未配置schema");
        }
        return ResData.success(info.getConfigSchame());
    }

    @GetMapping("/symbols")
    public ResData<List<Map<String, String>>> getSymbols(@RequestParam String type, @RequestParam(required = false) String keyword) {
        if (StrUtil.isBlank(type)) {
            return ResData.success(Collections.emptyList());
        }
        
        // 只有基金类型有数据表
        if ("FUND".equalsIgnoreCase(type) || "ETF".equalsIgnoreCase(type)) {
            List<Fund> funds = fundService.listAll();
            List<Map<String, String>> result = funds.stream()
                .filter(f -> {
                    boolean typeMatch = StrUtil.equalsIgnoreCase(f.getType(), type) ||
                            ("FUND".equalsIgnoreCase(type) && !"ETF".equalsIgnoreCase(f.getType()));
                    if (!typeMatch) return false;
                    if (StrUtil.isBlank(keyword)) return true;
                    return StrUtil.containsIgnoreCase(f.getSymbol(), keyword) || StrUtil.containsIgnoreCase(f.getName(), keyword);
                })
                .limit(50)
                .map(f -> {
                    java.util.HashMap<String, String> m = new java.util.HashMap<>();
                    String code = f.getSymbol();
                    String nm = f.getName();
                    m.put("value", code);
                    m.put("label", (code != null ? code : "") + " - " + (nm != null ? nm : ""));
                    return m;
                })
                .collect(Collectors.toList());
            return ResData.success(result);
        }
        
        // 其他类型暂无数据源，返回空列表
        return ResData.success(Collections.emptyList());
    }

    private List<StrategyInfoDto> buildDefaultStrategies() {
        java.util.ArrayList<StrategyInfoDto> result = new java.util.ArrayList<>();

        StrategyInfoDto ma = new StrategyInfoDto();
        ma.setClassName("MA_CROSS");
        ma.setName("双均线趋势跟踪");
        ma.setConfigSchame(buildMaCrossSchema().toString());
        ma.setConfig(new JSONObject()
                .set("shortWindow", 20)
                .set("longWindow", 60)
                .set("maxPosition", 0.8)
                .set("riskLimit", 0.1)
                .toString());
        result.add(ma);

        StrategyInfoDto vol = new StrategyInfoDto();
        vol.setClassName("VOL_TARGET");
        vol.setName("波动率目标");
        vol.setConfigSchame(buildVolTargetSchema().toString());
        vol.setConfig(new JSONObject()
                .set("targetVol", 0.15)
                .set("lookbackDays", 60)
                .set("maxLeverage", 2.0)
                .set("rebalanceFreq", "WEEKLY")
                .toString());
        result.add(vol);

        StrategyInfoDto mr = new StrategyInfoDto();
        mr.setClassName("MEAN_REVERSION");
        mr.setName("均值回归");
        mr.setConfigSchame(buildMeanReversionSchema().toString());
        mr.setConfig(new JSONObject()
                .set("lookbackDays", 30)
                .set("entryThreshold", 1.0)
                .set("exitThreshold", 0.3)
                .set("stopLoss", 0.15)
                .toString());
        result.add(mr);

        return result;
    }

    private JSONObject buildMaCrossSchema() {
        JSONObject schema = new JSONObject();
        schema.set("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.set("type", "object");
        JSONArray required = new JSONArray();
        required.add("shortWindow");
        required.add("longWindow");
        required.add("maxPosition");
        schema.set("required", required);
        JSONObject props = new JSONObject();
        props.set("shortWindow", new JSONObject().set("type", "integer").set("minimum", 1).set("maximum", 200));
        props.set("longWindow", new JSONObject().set("type", "integer").set("minimum", 2).set("maximum", 400));
        props.set("maxPosition", new JSONObject().set("type", "number").set("minimum", 0).set("maximum", 1));
        props.set("riskLimit", new JSONObject().set("type", "number").set("minimum", 0));
        schema.set("properties", props);
        return schema;
    }

    private JSONObject buildVolTargetSchema() {
        JSONObject schema = new JSONObject();
        schema.set("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.set("type", "object");
        JSONArray required = new JSONArray();
        required.add("targetVol");
        required.add("lookbackDays");
        schema.set("required", required);
        JSONObject props = new JSONObject();
        props.set("targetVol", new JSONObject().set("type", "number").set("minimum", 0.01).set("maximum", 0.5));
        props.set("lookbackDays", new JSONObject().set("type", "integer").set("minimum", 5).set("maximum", 365));
        props.set("maxLeverage", new JSONObject().set("type", "number").set("minimum", 1).set("maximum", 5));
        JSONArray freq = new JSONArray();
        freq.add("DAILY");
        freq.add("WEEKLY");
        freq.add("MONTHLY");
        props.set("rebalanceFreq", new JSONObject().set("type", "string").set("enum", freq));
        schema.set("properties", props);
        return schema;
    }

    private JSONObject buildMeanReversionSchema() {
        JSONObject schema = new JSONObject();
        schema.set("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.set("type", "object");
        JSONArray required = new JSONArray();
        required.add("lookbackDays");
        required.add("entryThreshold");
        required.add("exitThreshold");
        schema.set("required", required);
        JSONObject props = new JSONObject();
        props.set("lookbackDays", new JSONObject().set("type", "integer").set("minimum", 5).set("maximum", 365));
        props.set("entryThreshold", new JSONObject().set("type", "number").set("minimum", 0.5).set("maximum", 5));
        props.set("exitThreshold", new JSONObject().set("type", "number").set("minimum", 0.1).set("maximum", 5));
        props.set("stopLoss", new JSONObject().set("type", "number").set("minimum", 0).set("maximum", 0.3));
        schema.set("properties", props);
        return schema;
    }
}
