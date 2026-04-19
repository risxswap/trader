package cc.riskswap.trader.collector.repository.tushare;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cc.riskswap.trader.collector.common.model.query.FundNavQuery;
import cc.riskswap.trader.collector.common.util.DateUtil;
import cc.riskswap.trader.collector.common.util.NumberUtil;
import cc.riskswap.trader.base.dao.entity.FundNav;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class FundNavTushare {

    @Autowired
    private TushareManager tushareManager;

    public List<FundNav> list(FundNavQuery query) {
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
        if (query.getNavDate() != null) {
            params.put("nav_date", DateUtil.format(query.getNavDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }
        if (StringUtils.hasText(query.getMarket())) {
            params.put("market", query.getMarket());
        }
        if (query.getStartDate() != null) {
            params.put("start_date", DateUtil.format(query.getStartDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }
        if (query.getEndDate() != null) {
            params.put("end_date", DateUtil.format(query.getEndDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }

        String fields = "ts_code,ann_date,nav_date,unit_nav,accum_nav,accum_div,net_asset,total_netasset,adj_nav,update_flag";
        String response = tushareManager.post("fund_nav", fields, params);
        JSONObject jsonObj = JSON.parseObject(response);
        Integer code = jsonObj.getInteger("code");
        if (code != 0) {
            log.error("query fund nav data failed, response:{}", response);
            return Collections.emptyList();
        }
        if (!jsonObj.containsKey("data")) {
            return Collections.emptyList();
        }
        JSONObject data = jsonObj.getJSONObject("data");
        JSONArray items = data.getJSONArray("items");
        List<FundNav> fundNavs = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONArray item = items.getJSONArray(i);
            FundNav fundNav = toFundNav(item);
            fundNavs.add(fundNav);
        }
        return fundNavs;
    }

    private FundNav toFundNav(JSONArray item) {
        FundNav fundNav = new FundNav();
        fundNav.setCode(item.getString(0));
        fundNav.setTime(tushareManager.parseDate(item.getString(2)));
        fundNav.setUnitNav(NumberUtil.parseBigDecimal(item.get(3)));
        fundNav.setAccumNav(NumberUtil.parseBigDecimal(item.get(4)));
        fundNav.setAccumDiv(NumberUtil.parseBigDecimal(item.get(5)));
        fundNav.setNetAsset(NumberUtil.parseBigDecimal(item.get(6)));
        fundNav.setTotalNetAsset(NumberUtil.parseBigDecimal(item.get(7)));
        fundNav.setAdjNav(NumberUtil.parseBigDecimal(item.get(8)));
        // 设置创建和更新时间
        OffsetDateTime now = OffsetDateTime.now();
        fundNav.setCreatedAt(now);
        fundNav.setUpdatedAt(now);
        return fundNav;
    }
}
