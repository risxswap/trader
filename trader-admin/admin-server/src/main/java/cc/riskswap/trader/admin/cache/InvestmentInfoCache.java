package cc.riskswap.trader.admin.cache;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cc.riskswap.trader.admin.common.model.dto.InvestmentInfoDto;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InvestmentInfoCache {

    private static final String EXECUTOR_INVESTMENT_KEY = "EXECUTOR_INVESTMENT";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void cacheInvestment(List<InvestmentInfoDto> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        for (InvestmentInfoDto data : dataList) {
            if (data == null || data.getId() == null) {
                continue;
            }
            String json = JSONUtil.toJsonStr(data);
            if (json != null) {
                stringRedisTemplate.opsForHash().put(EXECUTOR_INVESTMENT_KEY, String.valueOf(data.getId()), json);
            }
        }
    }
}