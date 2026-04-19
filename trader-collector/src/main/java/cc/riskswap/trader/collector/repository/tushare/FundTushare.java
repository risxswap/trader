package cc.riskswap.trader.collector.repository.tushare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cc.riskswap.trader.collector.common.enums.ExchangeEnum;
import cc.riskswap.trader.collector.common.util.NumberUtil;
import cc.riskswap.trader.base.dao.entity.Fund;

@Component
public class FundTushare {

    @Autowired
    private TushareManager tushareManager;

    /**
     * 从tushare查询基金列表
     * @return
     */
    public List<Fund> list(Integer pageNo, Integer pageSize, String market, String status) {
        Map<String,Object> params = new HashMap<>();
        params.put("status", status);
        params.put("market", market);
        if (pageNo == null || pageNo<1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize<1) {
            pageSize = 20;
        }
        params.put("offset", (pageNo-1)*pageSize);
        params.put("limit", pageSize);
        String fileds = "ts_code,name,management,custodian,fund_type,found_date,due_date,list_date,issue_date,delist_date,issue_amount,m_fee,c_fee,duration_year,p_value,min_amount,exp_return,benchmark,status,invest_type,type,trustee,purc_startdate,redm_startdate,market";
        String response = tushareManager.post("fund_basic", fileds, params);
        JSONObject jsonObj = JSON.parseObject(response);
        if(!jsonObj.containsKey("data")) {
            return Collections.emptyList();
        }
        JSONObject data = jsonObj.getJSONObject("data");
        JSONArray items = data.getJSONArray("items");
        List<Fund> funds = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i++) {
            JSONArray item = items.getJSONArray(i);
            Fund fund = new Fund();
            
            // 设置所有字段，按照fields的顺序
            fund.setCode(item.getString(0));                    // ts_code
            fund.setName(item.getString(1));                    // name
            fund.setManagement(item.getString(2));              // management
            fund.setCustodian(item.getString(3));               // custodian
            fund.setFundType(item.getString(4));                // fund_type
            fund.setFoundDate(tushareManager.parseDate(item.getString(5)));    // found_date
            fund.setDueDate(tushareManager.parseDate(item.getString(6)));      // due_date
            fund.setListDate(tushareManager.parseDate(item.getString(7)));     // list_date
            fund.setIssueDate(tushareManager.parseDate(item.getString(8)));    // issue_date
            fund.setDelistDate(tushareManager.parseDate(item.getString(9)));   // delist_date
            fund.setIssueAmount(NumberUtil.parseBigDecimal(item.get(10))); // issue_amount
            fund.setMFee(NumberUtil.parseBigDecimal(item.get(11)));        // m_fee
            fund.setCFee(NumberUtil.parseBigDecimal(item.get(12)));        // c_fee
            fund.setDurationYear(NumberUtil.parseBigDecimal(item.get(13))); // duration_year
            fund.setPValue(NumberUtil.parseBigDecimal(item.get(14)));      // p_value
            fund.setMinAmount(NumberUtil.parseBigDecimal(item.get(15)));   // min_amount
            fund.setExpReturn(NumberUtil.parseBigDecimal(item.get(16)));   // exp_return
            fund.setBenchmark(item.getString(17));              // benchmark
            fund.setStatus(item.getString(18));                 // status
            fund.setInvestType(item.getString(19));             // invest_type
            fund.setType(item.getString(20));                   // type
            fund.setTrustee(item.getString(21));                // trustee
            fund.setPurcStartdate(tushareManager.parseDate(item.getString(22))); // purc_startdate
            fund.setRedmStartdate(tushareManager.parseDate(item.getString(23))); // redm_startdate
            fund.setMarket(item.getString(24));                // market
            if (fund.getCode().endsWith("SH")) {
                fund.setExchange(ExchangeEnum.SSE.code);
            } else if (fund.getCode().endsWith("SZ")) {
                fund.setExchange(ExchangeEnum.SZSE.code);
            }
            funds.add(fund);
        }
        return funds;
    }
}
