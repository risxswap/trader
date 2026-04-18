package cc.riskswap.trader.admin.cache;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cc.riskswap.trader.admin.common.model.dto.StrategyInfoDto;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StrategyInfoCache {

    private static final String STRATEGY_INFO_KEY = "STRATEGY_INFO";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void cacheStrategies(List<StrategyInfoDto> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            return;
        }
        for (StrategyInfoDto strategy : strategies) {
            if (strategy == null || strategy.getClassName() == null) {
                continue;
            }
            String json = JSONUtil.toJsonStr(strategy);
            if (json != null) {
                stringRedisTemplate.opsForHash().put(STRATEGY_INFO_KEY, strategy.getClassName(), json);
            }
        }
    }

    public StrategyInfoDto getStrategy(String className) {
        Object value = stringRedisTemplate.opsForHash().get(STRATEGY_INFO_KEY, className);
        if (value == null) {
            return null;
        }
        return JSONUtil.toBean((String) value, StrategyInfoDto.class);
    }

    public List<StrategyInfoDto> getAllStrategies() {
        List<Object> values = stringRedisTemplate.opsForHash().values(STRATEGY_INFO_KEY);
        if (values == null || values.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return values.stream()
                .map(v -> JSONUtil.toBean((String) v, StrategyInfoDto.class))
                .collect(java.util.stream.Collectors.toList());
    }
}
