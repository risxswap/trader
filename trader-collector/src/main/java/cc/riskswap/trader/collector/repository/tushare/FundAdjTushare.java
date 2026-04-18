package cc.riskswap.trader.collector.repository.tushare;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cc.riskswap.trader.collector.common.model.query.FundAdjQuery;
import cc.riskswap.trader.collector.common.util.DateUtil;
import cc.riskswap.trader.collector.repository.entity.FundAdj;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FundAdjTushare {

    @Autowired
    private TushareManager tushareManager;

    public List<FundAdj> list(FundAdjQuery query) {
        Map<String, Object> params = new HashMap<>();
        Integer pageNo = query.getPageNo();
        Integer pageSize = query.getPageSize();
        if (pageNo == null || pageNo<1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize<1) {
            pageSize = 20;
        }
        params.put("offset", (pageNo-1)*pageSize);
        params.put("limit", pageSize);
        if (StringUtils.hasText(query.getCode())) {
            params.put("ts_code", query.getCode());
        }
        if (query.getTradeDate()!=null) {
            params.put("trade_date", DateUtil.format(query.getTradeDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }
        if (query.getStartDate()!=null) {
            params.put("start_date", DateUtil.format(query.getStartDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }
        if (query.getEndDate()!=null) {
            params.put("end_date", DateUtil.format(query.getEndDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }
        
        String fields = "ts_code,trade_date,adj_factor";
        String response = tushareManager.post("fund_adj", fields, params);
        JSONObject jsonObj = JSON.parseObject(response);
        Integer code = jsonObj.getInteger("code");
        if (code!=0) {
            log.error("query fund adj data failed, response:{}", response);
            return Collections.emptyList();
        }
        if (!jsonObj.containsKey("data")) {
            return Collections.emptyList();
        }
        JSONObject data = jsonObj.getJSONObject("data");
        JSONArray items = data.getJSONArray("items");
        List<FundAdj> fundAdjs = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONArray item = items.getJSONArray(i);
            FundAdj fundAdj = new FundAdj();
            // 设置所有字段，按照fields的顺序
            fundAdj.setCode(item.getString(0));                  // ts_code
            String tradeDateStr = item.getString(1);
            tradeDateStr = tradeDateStr.replaceAll("`", "");
            fundAdj.setTime(tushareManager.parseDate(tradeDateStr));    // trade_date
            fundAdj.setAdjFactor(item.getDouble(2));         // adj_factor
            OffsetDateTime now = OffsetDateTime.now();
            fundAdj.setCreatedAt(now);
            fundAdj.setUpdatedAt(now);
            fundAdjs.add(fundAdj);
        }
        return fundAdjs;
    }

}
