package cc.riskswap.trader.admin.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cc.riskswap.trader.admin.common.model.dto.ExecutorInfoDto;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NodeInfoCache {

    private static final String EXECUTOR_STRATEGY_KEY = "EXECUTOR_INFO";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void cacheStrategy(String executorId, ExecutorInfoDto data) {
        if (data == null) {
            return;
        }
        String json = JSONUtil.toJsonStr(data);
        if (json != null) {
            stringRedisTemplate.opsForHash().put(EXECUTOR_STRATEGY_KEY, executorId, json);
        }
    }
}
