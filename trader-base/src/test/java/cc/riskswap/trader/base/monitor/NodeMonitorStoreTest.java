package cc.riskswap.trader.base.monitor;

import cc.riskswap.trader.base.dao.NodeMonitorDao;
import cc.riskswap.trader.base.dao.entity.NodeMonitor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeMonitorStoreTest {

    @Test
    void should_write_snapshot_to_clickhouse_and_redis() {
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        NodeMonitorStore store = new NodeMonitorStore(nodeMonitorDao, stringRedisTemplate);
        HardwareSnapshot snapshot = new HardwareSnapshot(
                "node-1",
                "collector",
                "采集器",
                OffsetDateTime.parse("2026-04-19T11:00:00Z"),
                "host-1",
                "10.0.0.1",
                new BigDecimal("0.12"),
                1000L,
                400L,
                2000L,
                800L,
                123L,
                11,
                22
        );

        store.write(snapshot);

        ArgumentCaptor<NodeMonitor> captor = ArgumentCaptor.forClass(NodeMonitor.class);
        Mockito.verify(nodeMonitorDao).save(captor.capture());
        NodeMonitor saved = captor.getValue();
        assertEquals("node-1", saved.getNodeId());
        assertEquals("collector", saved.getNodeType());
        assertEquals("采集器", saved.getNodeName());
        assertEquals(OffsetDateTime.parse("2026-04-19T11:00:00Z"), saved.getTimestamp());
        assertEquals("host-1", saved.getHostname());
        assertEquals("10.0.0.1", saved.getPrimaryIp());
        assertEquals(new BigDecimal("0.12"), saved.getCpuLoad());

        Mockito.verify(hashOperations).put(Mockito.eq("node:monitor"), Mockito.eq("node-1"), Mockito.anyString());
    }
}
