package cc.riskswap.trader.admin.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cc.riskswap.trader.admin.common.model.dto.HardwareInfoDto;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HardwareInfoCache {

    private static final String EXECUTOR_HARDWARE_KEY = "EXECUTOR_HARDWARE";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void cacheHardware(String executorId, HardwareInfoDto data) {
        if (data == null) {
            return;
        }
        String json = JSONUtil.toJsonStr(data);
        if (json != null) {
            stringRedisTemplate.opsForHash().put(EXECUTOR_HARDWARE_KEY, executorId, json);
        }
    }
}