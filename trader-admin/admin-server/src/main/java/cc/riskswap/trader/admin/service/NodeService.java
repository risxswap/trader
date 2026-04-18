package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.common.model.dto.NodeMetricsHistoryDto;
import cc.riskswap.trader.admin.common.model.dto.NodeGroupDto;
import cc.riskswap.trader.admin.common.model.dto.NodeStatusDto;
import cc.riskswap.trader.admin.common.model.param.NodeApproveParam;
import cc.riskswap.trader.admin.common.model.param.NodeGroupParam;
import cc.riskswap.trader.admin.common.model.param.NodeParam;
import cc.riskswap.trader.base.dao.NodeDao;
import cc.riskswap.trader.base.dao.NodeGroupDao;
import cc.riskswap.trader.base.dao.NodeMonitorDao;
import cc.riskswap.trader.base.dao.entity.Node;
import cc.riskswap.trader.base.dao.entity.NodeGroup;
import cc.riskswap.trader.base.dao.entity.NodeMonitor;
import cc.riskswap.trader.admin.exception.Warning;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final NodeMonitorDao nodeMonitorDao;
    private final NodeDao nodeDao;
    private final NodeGroupDao nodeGroupDao;

    private static final String NODE_MONITOR_KEY = "node:monitor";
    private static final String NODE_STATUS_PREFIX = "node:status:";
    private static final long OFFLINE_THRESHOLD_MS = 60000;
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";

    public List<NodeStatusDto> getAllNodes() {
        List<NodeGroup> groups = nodeGroupDao.list();
        Map<Long, NodeGroup> groupMap = groups.stream()
                .collect(Collectors.toMap(NodeGroup::getId, item -> item, (left, right) -> left));
        NodeGroup pendingGroup = groups.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsDefaultPending()))
                .findFirst()
                .orElseThrow(() -> new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "待审批分组不存在"));
        List<Node> metadataNodes = nodeDao.list();
        Map<String, Node> metadataMap = metadataNodes.stream()
                .filter(item -> StrUtil.isNotBlank(item.getNodeId()))
                .collect(Collectors.toMap(Node::getNodeId, item -> item, (left, right) -> left));
        Map<String, NodeStatusDto> nodeMap = new LinkedHashMap<>();

        Map<Object, Object> nodeMonitors = stringRedisTemplate.opsForHash().entries(NODE_MONITOR_KEY);
        if (nodeMonitors != null && !nodeMonitors.isEmpty()) {
            for (NodeStatusDto node : convertMonitorNodes(nodeMonitors)) {
                mergeMonitorNode(node, metadataMap, groupMap, pendingGroup);
                nodeMap.put(node.getNodeId(), node);
            }
        } else {
            Set<String> keys = stringRedisTemplate.keys(NODE_STATUS_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                long now = System.currentTimeMillis();
                for (String key : keys) {
                    String json = stringRedisTemplate.opsForValue().get(key);
                    if (json == null) {
                        continue;
                    }
                    NodeStatusDto node = JSONUtil.toBean(json, NodeStatusDto.class);
                    if (node.getTimestamp() != null) {
                        node.setOnline((now - node.getTimestamp()) <= OFFLINE_THRESHOLD_MS);
                    } else {
                        node.setOnline(false);
                    }
                    mergeMonitorNode(node, metadataMap, groupMap, pendingGroup);
                    nodeMap.put(node.getNodeId(), node);
                }
            }
        }

        for (Node metadataNode : metadataNodes) {
            if (nodeMap.containsKey(metadataNode.getNodeId())) {
                continue;
            }
            nodeMap.put(metadataNode.getNodeId(), buildOfflineNode(metadataNode, groupMap.get(metadataNode.getNodeGroupId())));
        }

        List<NodeStatusDto> nodes = new ArrayList<>(nodeMap.values());
        nodes.sort(Comparator.comparing(NodeStatusDto::getNodeId, Comparator.nullsLast(String::compareTo)));
        return nodes;
    }

    public NodeStatusDto getNode(Long id) {
        Node node = nodeDao.getById(id);
        if (node == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点不存在");
        }
        return getAllNodes().stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElseGet(() -> buildOfflineNode(node, findGroup(node.getNodeGroupId())));
    }

    public List<NodeGroupDto> getGroups() {
        Map<Long, Long> nodeCountMap = nodeDao.listAll().stream()
                .filter(item -> item.getNodeGroupId() != null)
                .collect(Collectors.groupingBy(Node::getNodeGroupId, Collectors.counting()));
        return nodeGroupDao.listAll().stream()
                .map(this::toGroupDto)
                .peek(item -> item.setNodeCount(nodeCountMap.getOrDefault(item.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public void addGroup(NodeGroupParam param) {
        validateDuplicateGroupCode(param.getCode(), null);
        OffsetDateTime now = OffsetDateTime.now();
        NodeGroup group = new NodeGroup();
        group.setName(param.getName());
        group.setCode(param.getCode());
        group.setSort(normalizeSort(param.getSort()));
        group.setIsDefaultPending(false);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        nodeGroupDao.save(group);
    }

    public void updateGroup(NodeGroupParam param) {
        NodeGroup group = nodeGroupDao.getById(param.getId());
        if (group == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点分组不存在");
        }
        if (Boolean.TRUE.equals(group.getIsDefaultPending())) {
            throw new Warning(ErrorCode.BAD_REQUEST.code(), "默认待审批分组不允许编辑");
        }
        validateDuplicateGroupCode(param.getCode(), group.getId());
        NodeGroup updateGroup = new NodeGroup();
        updateGroup.setId(group.getId());
        updateGroup.setName(param.getName());
        updateGroup.setCode(param.getCode());
        updateGroup.setSort(normalizeSort(param.getSort()));
        updateGroup.setUpdatedAt(OffsetDateTime.now());
        nodeGroupDao.updateById(updateGroup);
    }

    public void deleteGroup(Long id) {
        NodeGroup group = nodeGroupDao.getById(id);
        if (group == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点分组不存在");
        }
        if (Boolean.TRUE.equals(group.getIsDefaultPending())) {
            throw new Warning(ErrorCode.BAD_REQUEST.code(), "默认待审批分组不允许删除");
        }
        if (nodeDao.countByNodeGroupId(id) > 0) {
            throw new Warning(ErrorCode.BAD_REQUEST.code(), "分组下仍有节点，无法删除");
        }
        nodeGroupDao.removeById(id);
    }

    public void approve(NodeApproveParam param) {
        Node node = nodeDao.getById(param.getId());
        if (node == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点不存在");
        }
        NodeGroup group = nodeGroupDao.getById(param.getNodeGroupId());
        if (group == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点分组不存在");
        }
        Node updateNode = new Node();
        updateNode.setId(node.getId());
        updateNode.setNodeGroupId(group.getId());
        updateNode.setApprovalStatus(APPROVAL_APPROVED);
        updateNode.setApprovedAt(OffsetDateTime.now());
        updateNode.setUpdatedAt(OffsetDateTime.now());
        nodeDao.updateById(updateNode);
    }

    public void update(NodeParam param) {
        Node node = nodeDao.getById(param.getId());
        if (node == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点不存在");
        }
        NodeGroup group = nodeGroupDao.getById(param.getNodeGroupId());
        if (group == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点分组不存在");
        }
        Node updateNode = new Node();
        updateNode.setId(node.getId());
        updateNode.setNodeName(param.getNodeName());
        updateNode.setNodeType(param.getNodeType());
        updateNode.setNodeGroupId(param.getNodeGroupId());
        updateNode.setHostname(param.getHostname());
        updateNode.setPrimaryIp(param.getPrimaryIp());
        updateNode.setRemark(param.getRemark());
        updateNode.setUpdatedAt(OffsetDateTime.now());
        nodeDao.updateById(updateNode);
    }

    public void delete(Long id) {
        if (!nodeDao.removeById(id)) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "节点不存在");
        }
    }

    private List<NodeStatusDto> convertMonitorNodes(Map<Object, Object> nodeMonitors) {
        List<NodeStatusDto> nodes = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Object, Object> entry : nodeMonitors.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String json) || !JSONUtil.isTypeJSON(json)) {
                continue;
            }
            JSONObject monitor = JSONUtil.parseObj(json);
            NodeStatusDto node = new NodeStatusDto();
            node.setNodeId(monitor.getStr("nodeId", String.valueOf(entry.getKey())));
            node.setNodeName(monitor.getStr("nodeName", monitor.getStr("hostname", node.getNodeId())));
            node.setNodeType(monitor.getStr("nodeType", "collector"));
            node.setHostname(monitor.getStr("hostname", ""));
            node.setIpAddress(monitor.getStr("primaryIp", ""));
            node.setVersion(monitor.getStr("version", ""));
            node.setCpuUsage(toFloat(monitor.getDouble("cpuLoad")));
            node.setMemoryUsage(calculateUsageRatio(
                    monitor.getLong("physicalMemoryAvailable"),
                    monitor.getLong("physicalMemoryTotal")
            ));
            node.setDiskUsage(calculateUsageRatio(
                    monitor.getLong("diskAvailable"),
                    monitor.getLong("diskTotal")
            ));
            Long collectedAt = parseCollectedAtMillis(monitor.getStr("collectedAt"));
            node.setTimestamp(collectedAt);
            node.setOnline(collectedAt != null && (now - collectedAt) <= OFFLINE_THRESHOLD_MS);
            nodes.add(node);
        }
        return nodes;
    }

    private void mergeMonitorNode(
            NodeStatusDto node,
            Map<String, Node> metadataMap,
            Map<Long, NodeGroup> groupMap,
            NodeGroup pendingGroup
    ) {
        if (StrUtil.isBlank(node.getNodeId())) {
            return;
        }
        Node metadata = metadataMap.get(node.getNodeId());
        if (metadata == null) {
            metadata = registerNode(node, pendingGroup);
            metadataMap.put(metadata.getNodeId(), metadata);
            groupMap.putIfAbsent(pendingGroup.getId(), pendingGroup);
        }
        fillMetadata(node, metadata, groupMap.get(metadata.getNodeGroupId()));
    }

    private Node registerNode(NodeStatusDto node, NodeGroup pendingGroup) {
        OffsetDateTime now = OffsetDateTime.now();
        Node metadata = new Node();
        metadata.setNodeId(node.getNodeId());
        metadata.setNodeName(node.getNodeName());
        metadata.setNodeType(node.getNodeType());
        metadata.setNodeGroupId(pendingGroup.getId());
        metadata.setApprovalStatus(APPROVAL_PENDING);
        metadata.setHostname(node.getHostname());
        metadata.setPrimaryIp(node.getIpAddress());
        metadata.setCreatedAt(now);
        metadata.setUpdatedAt(now);
        nodeDao.save(metadata);
        return metadata;
    }

    private void fillMetadata(NodeStatusDto node, Node metadata, NodeGroup group) {
        node.setId(metadata.getId());
        node.setNodeGroupId(metadata.getNodeGroupId());
        node.setNodeGroupName(group != null ? group.getName() : "");
        node.setApprovalStatus(metadata.getApprovalStatus());
        node.setRemark(metadata.getRemark());
        if (StrUtil.isNotBlank(metadata.getNodeName())) {
            node.setNodeName(metadata.getNodeName());
        }
        if (StrUtil.isNotBlank(metadata.getNodeType())) {
            node.setNodeType(metadata.getNodeType());
        }
        if (StrUtil.isNotBlank(metadata.getHostname())) {
            node.setHostname(metadata.getHostname());
        }
        if (StrUtil.isNotBlank(metadata.getPrimaryIp())) {
            node.setIpAddress(metadata.getPrimaryIp());
        }
    }

    private NodeStatusDto buildOfflineNode(Node metadata, NodeGroup group) {
        NodeStatusDto node = new NodeStatusDto();
        node.setId(metadata.getId());
        node.setNodeId(metadata.getNodeId());
        node.setNodeName(metadata.getNodeName());
        node.setNodeType(metadata.getNodeType());
        node.setNodeGroupId(metadata.getNodeGroupId());
        node.setNodeGroupName(group != null ? group.getName() : "");
        node.setApprovalStatus(metadata.getApprovalStatus());
        node.setHostname(metadata.getHostname());
        node.setIpAddress(metadata.getPrimaryIp());
        node.setRemark(metadata.getRemark());
        node.setCpuUsage(0f);
        node.setMemoryUsage(0f);
        node.setDiskUsage(0f);
        node.setOnline(false);
        return node;
    }

    private NodeGroup findGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return nodeGroupDao.getById(groupId);
    }

    private NodeGroupDto toGroupDto(NodeGroup group) {
        NodeGroupDto dto = new NodeGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setCode(group.getCode());
        dto.setSort(group.getSort());
        dto.setDefaultPending(group.getIsDefaultPending());
        return dto;
    }

    private void validateDuplicateGroupCode(String code, Long currentId) {
        NodeGroup existingGroup = nodeGroupDao.getByCode(code);
        if (existingGroup == null) {
            return;
        }
        if (currentId != null && currentId.equals(existingGroup.getId())) {
            return;
        }
        throw new Warning(ErrorCode.BAD_REQUEST.code(), "分组编码已存在");
    }

    private Integer normalizeSort(Integer sort) {
        return sort == null ? 0 : sort;
    }

    private Float calculateUsageRatio(Long available, Long total) {
        if (available == null || total == null || total <= 0) {
            return 0f;
        }
        double usage = (double) (total - available) / total;
        if (usage < 0) {
            return 0f;
        }
        if (usage > 1) {
            return 1f;
        }
        return (float) usage;
    }

    private Float toFloat(Double value) {
        if (value == null) {
            return 0f;
        }
        if (value < 0) {
            return 0f;
        }
        if (value > 1) {
            return 1f;
        }
        return value.floatValue();
    }

    private Long parseCollectedAtMillis(String collectedAt) {
        if (collectedAt == null || collectedAt.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(collectedAt).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.warn("node monitor collectedAt parse failed: {}", collectedAt);
            return null;
        }
    }

    public NodeMetricsHistoryDto getNodeHistory(String nodeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<NodeMonitor> logs = nodeMonitorDao.listHistory(nodeId, startTime, endTime);

        NodeMetricsHistoryDto history = new NodeMetricsHistoryDto();
        List<String> timestamps = new ArrayList<>();
        List<Float> cpuUsages = new ArrayList<>();
        List<Float> memoryUsages = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

        for (NodeMonitor log : logs) {
            timestamps.add(formatter.format(log.getTimestamp()));
            cpuUsages.add(log.getCpuUsage() != null ? log.getCpuUsage() : 0f);
            memoryUsages.add(log.getMemoryUsage() != null ? log.getMemoryUsage() : 0f);
        }

        history.setTimestamps(timestamps);
        history.setCpuUsages(cpuUsages);
        history.setMemoryUsages(memoryUsages);

        return history;
    }
}
