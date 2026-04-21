package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.config.TraderNodeProperties;
import cc.riskswap.trader.base.dao.NodeMonitorDao;
import cc.riskswap.trader.base.dao.mapper.NodeMonitorMapper;
import cc.riskswap.trader.base.monitor.HardwareMonitorPublisher;
import cc.riskswap.trader.base.monitor.HardwareMonitorService;
import cc.riskswap.trader.base.monitor.NodeMonitorStore;
import cc.riskswap.trader.base.monitor.TraderMonitorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TraderMonitorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TraderMonitorAutoConfiguration.class))
            .withBean(NodeMonitorDao.class, () -> mock(NodeMonitorDao.class))
            .withBean(NodeMonitorMapper.class, () -> mock(NodeMonitorMapper.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withPropertyValues(
                    "trader.monitor.enabled=true",
                    "trader.node.id=node-1",
                    "trader.node.type=collector",
                    "trader.node.name=test-node"
            );

    @Test
    void should_register_monitor_beans_when_monitoring_is_enabled() {
        TraderMonitorAutoConfiguration configuration = new TraderMonitorAutoConfiguration();
        TraderNodeProperties nodeProperties = new TraderNodeProperties();
        nodeProperties.setId("node-1");
        nodeProperties.setType("collector");
        nodeProperties.setName("采集器");
        TraderMonitorProperties monitorProperties = new TraderMonitorProperties();
        monitorProperties.setEnabled(true);

        HardwareMonitorService hardwareMonitorService = configuration.hardwareMonitorService(nodeProperties);
        NodeMonitorStore nodeMonitorStore = configuration.nodeMonitorStore(mock(NodeMonitorDao.class), mock(StringRedisTemplate.class));
        HardwareMonitorPublisher hardwareMonitorPublisher = configuration.hardwareMonitorPublisher(
                hardwareMonitorService,
                nodeMonitorStore,
                monitorProperties
        );

        assertThat(hardwareMonitorService).isNotNull();
        assertThat(nodeMonitorStore).isNotNull();
        assertThat(hardwareMonitorPublisher).isNotNull();
    }

    @Test
    void should_register_named_trader_monitor_properties_bean_for_scheduling_expression() {
        contextRunner.run(context -> assertThat(context).hasBean("traderMonitorProperties"));
    }
}
