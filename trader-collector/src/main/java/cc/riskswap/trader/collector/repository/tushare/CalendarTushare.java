package cc.riskswap.trader.collector.repository.tushare;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cc.riskswap.trader.collector.common.model.query.CalendarQuery;
import cc.riskswap.trader.collector.common.util.DateUtil;
import cc.riskswap.trader.collector.repository.entity.Calendar;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CalendarTushare {

    @Autowired
    private TushareManager tushareManager;

    public List<Calendar> list(CalendarQuery query) {
        Map<String,Object> params = new HashMap<>();
        if (query.getExchange() != null) {
            params.put("exchange", query.getExchange());
        }
        if (query.getStartDate() != null) {
            params.put("start_date", DateUtil.format(query.getStartDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }
        if (query.getEndDate() != null) {
            params.put("end_date", DateUtil.format(query.getEndDate(), DateUtil.D_FORMAT_INT_FORMATTER));
        }
        if (query.getIsOpen() != null) {
            params.put("is_open", query.getIsOpen());
        }

        String fields = "exchange,cal_date,is_open,pretrade_date";
        String res = tushareManager.post("trade_cal", fields, params);

        JSONObject jsonObj = JSON.parseObject(res);
        Integer code = jsonObj.getInteger("code");
        if (code != null && code != 0) {
            log.error("query trade_cal failed, response:{}", res);
            return new ArrayList<>();
        }
        if (!jsonObj.containsKey("data")) {
            return new ArrayList<>();
        }
        JSONObject data = jsonObj.getJSONObject("data");
        JSONArray items = data.getJSONArray("items");
        List<Calendar> calendars = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONArray item = items.getJSONArray(i);
            Calendar calendar = new Calendar();
            // 按照 fields 的顺序设置字段
            calendar.setExchange(item.getString(0)); // exchange

            String calDateStr = item.getString(1);
            if (calDateStr != null) {
                calDateStr = calDateStr.replaceAll("`", "");
                try {
                    calendar.setDate(LocalDate.parse(calDateStr, DateUtil.D_FORMAT_INT_FORMATTER)); // cal_date
                } catch (Exception e) {
                    log.warn("parse cal_date failed: {}", calDateStr);
                }
            }

            Integer isOpen = item.getInteger(2);
            calendar.setOpen(isOpen == null ? 0 : isOpen); // is_open

            String preDateStr = item.getString(3);
            if (preDateStr != null && !preDateStr.isEmpty()) {
                preDateStr = preDateStr.replaceAll("`", "");
                try {
                    calendar.setPreDate(LocalDate.parse(preDateStr, DateUtil.D_FORMAT_INT_FORMATTER)); // pretrade_date
                } catch (Exception e) {
                    log.warn("parse pretrade_date failed: {}", preDateStr);
                }
            }
            calendar.setCreatedAt(OffsetDateTime.now());
            calendar.setUpdatedAt(OffsetDateTime.now());
            calendars.add(calendar);
        }
        return calendars;
    }
}
