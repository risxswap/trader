package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.common.model.param.NodeApproveParam;
import cc.riskswap.trader.admin.common.model.param.NodeGroupParam;
import cc.riskswap.trader.admin.common.model.dto.NodeStatusDto;
import cc.riskswap.trader.base.dao.NodeDao;
import cc.riskswap.trader.base.dao.NodeGroupDao;
import cc.riskswap.trader.base.dao.NodeMonitorDao;
import cc.riskswap.trader.base.dao.entity.Node;
import cc.riskswap.trader.base.dao.entity.NodeGroup;
import cc.riskswap.trader.base.dao.entity.NodeMonitor;
import cc.riskswap.trader.admin.service.NodeService;
import cc.riskswap.trader.admin.exception.Warning;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NodeServiceTest {

    @Test
    void shouldReadNodeMonitorHashAndConvertFields() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        long collectedAt = System.currentTimeMillis() - 10_000L;
        String monitorJson = """
                {
                  "nodeId":"a55eca68-5ed1-4ac8-8c94-01da1eef4faa",
                  "nodeType":"executor",
                  "nodeName":"执行器",
                  "collectedAt":%d,
                  "hostname":"ca04f23abf49",
                  "primaryIp":"172.22.0.2",
                  "cpuLoad":0.0103,
                  "physicalMemoryTotal":16154451968,
                  "physicalMemoryAvailable":9997930496,
                  "diskTotal":5002351247360,
                  "diskAvailable":2072913510400
                }
                """.formatted(collectedAt);

        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        Mockito.when(hashOperations.entries("node:monitor"))
                .thenReturn(Map.of("a55eca68-5ed1-4ac8-8c94-01da1eef4faa", monitorJson));
        Mockito.when(nodeDao.list()).thenReturn(Collections.emptyList());
        Mockito.when(nodeGroupDao.list()).thenReturn(List.of(pendingGroup()));

        List<NodeStatusDto> result = nodeService.getAllNodes();

        Assertions.assertEquals(1, result.size());
        NodeStatusDto node = result.get(0);
        Assertions.assertEquals("a55eca68-5ed1-4ac8-8c94-01da1eef4faa", node.getNodeId());
        Assertions.assertEquals("执行器", node.getNodeName());
        Assertions.assertEquals("executor", node.getNodeType());
        Assertions.assertEquals("172.22.0.2", node.getIpAddress());
        Assertions.assertEquals(0.0103f, node.getCpuUsage(), 0.0001f);
        Assertions.assertEquals(0.3811f, node.getMemoryUsage(), 0.0001f);
        Assertions.assertEquals(0.5856f, node.getDiskUsage(), 0.0001f);
        Assertions.assertTrue(node.getOnline());
        Assertions.assertEquals(collectedAt, node.getTimestamp());
        Assertions.assertEquals("待审批", node.getNodeGroupName());
        Assertions.assertEquals("PENDING", node.getApprovalStatus());
    }

    @Test
    void shouldTreatNumericCollectedAtInNodeMonitorHashAsOnline() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        long collectedAt = System.currentTimeMillis() - 10_000L;
        String monitorJson = """
                {
                  "nodeId":"numeric-monitor-node",
                  "nodeType":"statistic",
                  "nodeName":"统计服务",
                  "collectedAt":%d,
                  "hostname":"statistic-host",
                  "primaryIp":"172.31.0.2",
                  "cpuLoad":0.0113,
                  "physicalMemoryTotal":16154390528,
                  "physicalMemoryAvailable":631619584,
                  "diskTotal":4542971850752,
                  "diskAvailable":1547027345408
                }
                """.formatted(collectedAt);

        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        Mockito.when(hashOperations.entries("node:monitor"))
                .thenReturn(Map.of("numeric-monitor-node", monitorJson));
        Mockito.when(nodeDao.list()).thenReturn(Collections.emptyList());
        Mockito.when(nodeGroupDao.list()).thenReturn(List.of(pendingGroup()));

        List<NodeStatusDto> result = nodeService.getAllNodes();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(collectedAt, result.get(0).getTimestamp());
        Assertions.assertTrue(result.get(0).getOnline());
    }

    @Test
    void shouldFallbackToLegacyNodeStatusKeysWhenMonitorHashEmpty() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        long now = System.currentTimeMillis();
        String legacyJson = """
                {
                  "nodeId":"legacy-node",
                  "nodeName":"legacy-host",
                  "nodeType":"EXECUTOR",
                  "ipAddress":"10.0.0.1",
                  "cpuUsage":0.2,
                  "memoryUsage":0.3,
                  "diskUsage":0.4,
                  "timestamp":%d
                }
                """.formatted(now);

        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        Mockito.when(hashOperations.entries("node:monitor")).thenReturn(Collections.emptyMap());
        Mockito.when(stringRedisTemplate.keys("node:status:*")).thenReturn(Set.of("node:status:legacy-node"));
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.get("node:status:legacy-node")).thenReturn(legacyJson);
        Mockito.when(nodeDao.list()).thenReturn(Collections.emptyList());
        Mockito.when(nodeGroupDao.list()).thenReturn(List.of(pendingGroup()));

        List<NodeStatusDto> result = nodeService.getAllNodes();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("legacy-node", result.get(0).getNodeId());
        Assertions.assertTrue(result.get(0).getOnline());
    }

    @Test
    void shouldBuildNodeHistoryFromClickHouseLogs() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        OffsetDateTime start = OffsetDateTime.parse("2026-04-11T15:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-04-11T16:00:00Z");
        OffsetDateTime t1 = OffsetDateTime.parse("2026-04-11T15:13:45.089Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2026-04-11T15:14:45.089Z");

        NodeMonitor l1 = new NodeMonitor();
        l1.setTimestamp(t1);
        l1.setCpuUsage(0.0103f);
        l1.setMemoryUsage(0.3811f);
        NodeMonitor l2 = new NodeMonitor();
        l2.setTimestamp(t2);
        l2.setCpuUsage(0.02f);
        l2.setMemoryUsage(0.41f);

        Mockito.when(nodeMonitorDao.listHistory("node-1", start, end)).thenReturn(List.of(l1, l2));

        cc.riskswap.trader.admin.common.model.dto.NodeMetricsHistoryDto result =
                nodeService.getNodeHistory("node-1", start, end);

        Mockito.verify(nodeMonitorDao).listHistory("node-1", start, end);
        Assertions.assertEquals(2, result.getTimestamps().size());
        Assertions.assertEquals(2, result.getCpuUsages().size());
        Assertions.assertEquals(2, result.getMemoryUsages().size());
        Assertions.assertEquals(0.0103f, result.getCpuUsages().get(0), 0.0001f);
        Assertions.assertEquals(0.3811f, result.getMemoryUsages().get(0), 0.0001f);
    }

    @Test
    void shouldMergeMysqlMetadataAndRegisterNewNodeToPendingGroup() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        NodeGroup pendingGroup = pendingGroup();
        NodeGroup prodGroup = new NodeGroup();
        prodGroup.setId(2L);
        prodGroup.setName("生产组");

        Node existingNode = new Node();
        existingNode.setId(10L);
        existingNode.setNodeId("existing-node");
        existingNode.setNodeName("已编辑执行器");
        existingNode.setNodeType("executor");
        existingNode.setNodeGroupId(2L);
        existingNode.setApprovalStatus("APPROVED");
        existingNode.setHostname("executor-host");
        existingNode.setPrimaryIp("10.0.0.2");

        long collectedAt = System.currentTimeMillis() - 10_000L;
        String existingMonitorJson = """
                {
                  "nodeId":"existing-node",
                  "nodeType":"collector",
                  "nodeName":"采集器上报名称",
                  "collectedAt":%d,
                  "hostname":"executor-host",
                  "primaryIp":"10.0.0.2",
                  "cpuLoad":0.12,
                  "physicalMemoryTotal":100,
                  "physicalMemoryAvailable":40,
                  "diskTotal":100,
                  "diskAvailable":20
                }
                """.formatted(collectedAt);
        String newMonitorJson = """
                {
                  "nodeId":"new-node",
                  "nodeType":"collector",
                  "nodeName":"新采集器",
                  "collectedAt":%d,
                  "hostname":"collector-host",
                  "primaryIp":"10.0.0.3",
                  "cpuLoad":0.22,
                  "physicalMemoryTotal":200,
                  "physicalMemoryAvailable":100,
                  "diskTotal":100,
                  "diskAvailable":50
                }
                """.formatted(collectedAt);

        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        Mockito.when(hashOperations.entries("node:monitor"))
                .thenReturn(Map.of("existing-node", existingMonitorJson, "new-node", newMonitorJson));
        Mockito.when(nodeDao.list()).thenReturn(List.of(existingNode));
        Mockito.when(nodeGroupDao.list()).thenReturn(List.of(pendingGroup, prodGroup));

        List<NodeStatusDto> result = nodeService.getAllNodes();

        Assertions.assertEquals(2, result.size());
        NodeStatusDto existingResult = result.stream()
                .filter(item -> "existing-node".equals(item.getNodeId()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(10L, existingResult.getId());
        Assertions.assertEquals("已编辑执行器", existingResult.getNodeName());
        Assertions.assertEquals("executor", existingResult.getNodeType());
        Assertions.assertEquals("生产组", existingResult.getNodeGroupName());
        Assertions.assertEquals("APPROVED", existingResult.getApprovalStatus());

        NodeStatusDto newResult = result.stream()
                .filter(item -> "new-node".equals(item.getNodeId()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("新采集器", newResult.getNodeName());
        Assertions.assertEquals("待审批", newResult.getNodeGroupName());
        Assertions.assertEquals("PENDING", newResult.getApprovalStatus());

        ArgumentCaptor<Node> captor = ArgumentCaptor.forClass(Node.class);
        Mockito.verify(nodeDao).save(captor.capture());
        Node savedNode = captor.getValue();
        Assertions.assertEquals("new-node", savedNode.getNodeId());
        Assertions.assertEquals("新采集器", savedNode.getNodeName());
        Assertions.assertEquals("collector", savedNode.getNodeType());
        Assertions.assertEquals(1L, savedNode.getNodeGroupId());
        Assertions.assertEquals("PENDING", savedNode.getApprovalStatus());
    }

    @Test
    void shouldApproveNodeAndMoveToSelectedGroup() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        Node node = new Node();
        node.setId(100L);
        node.setNodeId("pending-node");
        node.setApprovalStatus("PENDING");
        node.setNodeGroupId(1L);

        NodeGroup group = new NodeGroup();
        group.setId(3L);
        group.setName("执行器分组");

        NodeApproveParam param = new NodeApproveParam();
        param.setId(100L);
        param.setNodeGroupId(3L);

        Mockito.when(nodeDao.getById(100L)).thenReturn(node);
        Mockito.when(nodeGroupDao.getById(3L)).thenReturn(group);

        nodeService.approve(param);

        ArgumentCaptor<Node> captor = ArgumentCaptor.forClass(Node.class);
        Mockito.verify(nodeDao).updateById(captor.capture());
        Node updatedNode = captor.getValue();
        Assertions.assertEquals(100L, updatedNode.getId());
        Assertions.assertEquals(3L, updatedNode.getNodeGroupId());
        Assertions.assertEquals("APPROVED", updatedNode.getApprovalStatus());
        Assertions.assertNotNull(updatedNode.getApprovedAt());
    }

    @Test
    void shouldIgnoreDeletedNodeEvenWhenMonitorStillReported() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        Node deletedNode = new Node();
        deletedNode.setId(101L);
        deletedNode.setNodeId("deleted-node");
        deletedNode.setNodeName("已删除节点");
        deletedNode.setNodeType("collector");
        deletedNode.setNodeGroupId(1L);
        deletedNode.setApprovalStatus("DELETED");

        long collectedAt = System.currentTimeMillis() - 10_000L;
        String monitorJson = """
                {
                  "nodeId":"deleted-node",
                  "nodeType":"collector",
                  "nodeName":"仍在上报的节点",
                  "collectedAt":%d,
                  "hostname":"collector-host",
                  "primaryIp":"10.0.0.9",
                  "cpuLoad":0.22,
                  "physicalMemoryTotal":200,
                  "physicalMemoryAvailable":100,
                  "diskTotal":100,
                  "diskAvailable":50
                }
                """.formatted(collectedAt);

        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        Mockito.when(hashOperations.entries("node:monitor"))
                .thenReturn(Map.of("deleted-node", monitorJson));
        Mockito.when(nodeDao.list()).thenReturn(List.of(deletedNode));
        Mockito.when(nodeGroupDao.list()).thenReturn(List.of(pendingGroup()));

        List<NodeStatusDto> result = nodeService.getAllNodes();

        Assertions.assertTrue(result.isEmpty());
        Mockito.verify(nodeDao, Mockito.never()).save(Mockito.any(Node.class));
    }

    @Test
    void shouldMarkNodeDeletedAndClearRealtimeCaches() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        Node node = new Node();
        node.setId(100L);
        node.setNodeId("node-to-delete");
        node.setApprovalStatus("APPROVED");

        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        Mockito.when(nodeDao.getById(100L)).thenReturn(node);

        nodeService.delete(100L);

        ArgumentCaptor<Node> captor = ArgumentCaptor.forClass(Node.class);
        Mockito.verify(nodeDao).updateById(captor.capture());
        Node updatedNode = captor.getValue();
        Assertions.assertEquals(100L, updatedNode.getId());
        Assertions.assertEquals("DELETED", updatedNode.getApprovalStatus());
        Assertions.assertNotNull(updatedNode.getUpdatedAt());
        Mockito.verify(hashOperations).delete("node:monitor", "node-to-delete");
        Mockito.verify(stringRedisTemplate).delete("node:status:node-to-delete");
    }

    @Test
    void shouldCreateNodeGroup() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        NodeGroupParam param = new NodeGroupParam();
        param.setName("生产节点");
        param.setCode("prod");
        param.setSort(10);

        nodeService.addGroup(param);

        ArgumentCaptor<NodeGroup> captor = ArgumentCaptor.forClass(NodeGroup.class);
        Mockito.verify(nodeGroupDao).save(captor.capture());
        NodeGroup savedGroup = captor.getValue();
        Assertions.assertEquals("生产节点", savedGroup.getName());
        Assertions.assertEquals("prod", savedGroup.getCode());
        Assertions.assertEquals(10, savedGroup.getSort());
        Assertions.assertEquals(Boolean.FALSE, savedGroup.getIsDefaultPending());
        Assertions.assertNotNull(savedGroup.getCreatedAt());
    }

    @Test
    void shouldUpdateNodeGroup() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        NodeGroup group = new NodeGroup();
        group.setId(2L);
        group.setName("旧分组");
        group.setCode("old");
        group.setIsDefaultPending(false);

        NodeGroupParam param = new NodeGroupParam();
        param.setId(2L);
        param.setName("新分组");
        param.setCode("new");
        param.setSort(20);

        Mockito.when(nodeGroupDao.getById(2L)).thenReturn(group);

        nodeService.updateGroup(param);

        ArgumentCaptor<NodeGroup> captor = ArgumentCaptor.forClass(NodeGroup.class);
        Mockito.verify(nodeGroupDao).updateById(captor.capture());
        NodeGroup updatedGroup = captor.getValue();
        Assertions.assertEquals(2L, updatedGroup.getId());
        Assertions.assertEquals("新分组", updatedGroup.getName());
        Assertions.assertEquals("new", updatedGroup.getCode());
        Assertions.assertEquals(20, updatedGroup.getSort());
    }

    @Test
    void shouldRejectDeletingDefaultPendingGroup() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        Mockito.when(nodeGroupDao.getById(1L)).thenReturn(pendingGroup());

        Assertions.assertThrows(Warning.class, () -> nodeService.deleteGroup(1L));
        Mockito.verify(nodeGroupDao, Mockito.never()).removeById(Mockito.anyLong());
    }

    @Test
    void shouldRejectDeletingGroupWhenNodesExist() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        NodeGroup group = new NodeGroup();
        group.setId(2L);
        group.setName("生产组");
        group.setIsDefaultPending(false);

        Mockito.when(nodeGroupDao.getById(2L)).thenReturn(group);
        Mockito.when(nodeDao.countByNodeGroupIdExcludingApprovalStatus(2L, "DELETED")).thenReturn(2L);

        Assertions.assertThrows(Warning.class, () -> nodeService.deleteGroup(2L));
        Mockito.verify(nodeGroupDao, Mockito.never()).removeById(Mockito.anyLong());
    }

    @Test
    void shouldDeleteEmptyGroup() {
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeGroupDao nodeGroupDao = Mockito.mock(NodeGroupDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        NodeService nodeService = new NodeService(stringRedisTemplate, nodeMonitorDao, nodeDao, nodeGroupDao);

        NodeGroup group = new NodeGroup();
        group.setId(3L);
        group.setName("空分组");
        group.setIsDefaultPending(false);

        Mockito.when(nodeGroupDao.getById(3L)).thenReturn(group);
        Mockito.when(nodeDao.countByNodeGroupIdExcludingApprovalStatus(3L, "DELETED")).thenReturn(0L);

        nodeService.deleteGroup(3L);

        Mockito.verify(nodeGroupDao).removeById(3L);
    }

    private NodeGroup pendingGroup() {
        NodeGroup pendingGroup = new NodeGroup();
        pendingGroup.setId(1L);
        pendingGroup.setName("待审批");
        pendingGroup.setCode("pending");
        pendingGroup.setIsDefaultPending(true);
        return pendingGroup;
    }
}
