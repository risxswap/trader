package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.config.TraderNodeProperties;
import cc.riskswap.trader.base.dao.NodeMonitorDao;
import cc.riskswap.trader.base.monitor.HardwareMonitorPublisher;
import cc.riskswap.trader.base.monitor.HardwareMonitorService;
import cc.riskswap.trader.base.monitor.NodeMonitorStore;
import cc.riskswap.trader.base.monitor.TraderMonitorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "trader.monitor", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TraderNodeProperties.class)
public class TraderMonitorAutoConfiguration {

    @Bean("traderMonitorProperties")
    @ConditionalOnMissingBean(name = "traderMonitorProperties")
    @ConfigurationProperties(prefix = "trader.monitor")
    public TraderMonitorProperties traderMonitorProperties() {
        return new TraderMonitorProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public HardwareMonitorService hardwareMonitorService(TraderNodeProperties traderNodeProperties) {
        return new HardwareMonitorService(traderNodeProperties);
    }

    @Bean
    @ConditionalOnBean({NodeMonitorDao.class, StringRedisTemplate.class})
    @ConditionalOnMissingBean
    public NodeMonitorStore nodeMonitorStore(NodeMonitorDao nodeMonitorDao, StringRedisTemplate stringRedisTemplate) {
        return new NodeMonitorStore(nodeMonitorDao, stringRedisTemplate);
    }

    @Bean
    @ConditionalOnBean({HardwareMonitorService.class, NodeMonitorStore.class})
    @ConditionalOnMissingBean
    public HardwareMonitorPublisher hardwareMonitorPublisher(
            HardwareMonitorService hardwareMonitorService,
            NodeMonitorStore nodeMonitorStore,
            TraderMonitorProperties traderMonitorProperties
    ) {
        return new HardwareMonitorPublisher(hardwareMonitorService, nodeMonitorStore, traderMonitorProperties);
    }
}
