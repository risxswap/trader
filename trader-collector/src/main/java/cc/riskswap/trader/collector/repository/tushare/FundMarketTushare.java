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

import cc.riskswap.trader.collector.common.enums.TimeFrameEnum;
import cc.riskswap.trader.collector.common.model.query.FundMarketQuery;
import cc.riskswap.trader.collector.common.util.DateUtil;
import cc.riskswap.trader.collector.common.util.NumberUtil;
import cc.riskswap.trader.collector.repository.entity.FundMarket;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FundMarketTushare {

    @Autowired
    private TushareManager tushareManager;

    public List<FundMarket> list(FundMarketQuery query) {
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
        
        String fields = "ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount";
        String response = tushareManager.post("fund_daily", fields, params);
        JSONObject jsonObj = JSON.parseObject(response);
        Integer code = jsonObj.getInteger("code");
        if (code!=0) {
            log.error("query fund market data failed, response:{}", response);
            return Collections.emptyList();
        }
        if (!jsonObj.containsKey("data")) {
            return Collections.emptyList();
        }
        JSONObject data = jsonObj.getJSONObject("data");
        JSONArray items = data.getJSONArray("items");
        List<FundMarket> fundMarkets = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONArray item = items.getJSONArray(i);
            FundMarket fundMarket = new FundMarket();
            // 设置所有字段，按照fields的顺序
            fundMarket.setCode(item.getString(0));                  // ts_code
            String tradeDateStr = item.getString(1);
            tradeDateStr = tradeDateStr.replaceAll("`", "");
            fundMarket.setTime(tushareManager.parseDate(tradeDateStr));    // trade_date
            fundMarket.setOpen(NumberUtil.parseBigDecimal(item.get(2)));         // open
            fundMarket.setHigh(NumberUtil.parseBigDecimal(item.get(3)));         // high
            fundMarket.setLow(NumberUtil.parseBigDecimal(item.get(4)));          // low
            fundMarket.setClose(NumberUtil.parseBigDecimal(item.get(5)));        // close
            fundMarket.setPreClose(NumberUtil.parseBigDecimal(item.get(6)));     // pre_close
            fundMarket.setChange(NumberUtil.parseBigDecimal(item.get(7)));       // change
            fundMarket.setPctChg(NumberUtil.parseBigDecimal(item.get(8)));       // pct_chg
            fundMarket.setVol(NumberUtil.parseBigDecimal(item.get(9)));          // vol
            fundMarket.setAmount(NumberUtil.parseBigDecimal(item.get(10)));      // amount
            fundMarket.setTimeFrame(TimeFrameEnum.D1.code);
            // 设置创建和更新时间
            OffsetDateTime now = OffsetDateTime.now();
            fundMarket.setCreatedAt(now);
            fundMarket.setUpdatedAt(now);
            fundMarkets.add(fundMarket);
        }
        return fundMarkets;
    }
}
