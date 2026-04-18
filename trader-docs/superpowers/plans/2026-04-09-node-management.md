# Node Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Node Management module to display real-time node status from Redis and historical hardware metrics from ClickHouse.

**Architecture:** 
The backend will use Spring Boot with RedisTemplate for real-time status and MyBatis-Plus (with ClickHouse JDBC) for querying historical metrics. The frontend will be built with Vue 3, Element Plus, and ECharts to display the node list and historical usage trends. A scheduled task or direct DB migration will be needed to create the ClickHouse table.

**Tech Stack:** Java 17, Spring Boot, Redis, ClickHouse, Vue 3, Element Plus, ECharts.

---

### Task 1: Create ClickHouse Table and Backend Models

**Files:**
- Create/Modify: `admin-server/src/main/resources/db/clickhouse.sql` (if exists, else create migration)
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/entity/NodeMetricsLog.java`
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/NodeStatusDto.java`
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/NodeMetricsHistoryDto.java`

- [ ] **Step 1: Create ClickHouse table schema**
Add to the ClickHouse init script:
```sql
CREATE TABLE IF NOT EXISTS node_metrics_log
(
    timestamp DateTime,
    node_id String,
    node_name String,
    node_type String,
    ip_address String,
    version String,
    cpu_usage Float32,
    memory_usage Float32,
    disk_usage Float32
) ENGINE = MergeTree()
ORDER BY (node_id, timestamp);
```

- [ ] **Step 2: Create NodeStatusDto**
```java
package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class NodeStatusDto {
    private String nodeId;
    private String nodeName;
    private String nodeType; // EXECUTOR / COLLECTOR
    private String ipAddress;
    private String version;
    private Float cpuUsage;
    private Float memoryUsage;
    private Float diskUsage;
    private Long timestamp;
    private Boolean online;
}
```

- [ ] **Step 3: Create NodeMetricsHistoryDto**
```java
package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class NodeMetricsHistoryDto {
    private List<String> timestamps;
    private List<Float> cpuUsages;
    private List<Float> memoryUsages;
}
```

- [ ] **Step 4: Create NodeMetricsLog Entity**
```java
package cc.riskswap.trader.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("node_metrics_log")
public class NodeMetricsLog {
    private OffsetDateTime timestamp;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private String ipAddress;
    private String version;
    private Float cpuUsage;
    private Float memoryUsage;
    private Float diskUsage;
}
```

- [ ] **Step 5: Commit**
```bash
git add admin-server/src/main/resources/db admin-server/src/main/java/cc/riskswap/trader/admin/dao/entity admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto
git commit -m "feat: add clickhouse schema and node models"
```

---

### Task 2: Implement Backend Dao and Service

**Files:**
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/NodeMetricsLogMapper.java`
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/NodeMetricsLogDao.java`
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/service/NodeService.java`

- [ ] **Step 1: Create Mapper and Dao**
Create `NodeMetricsLogMapper.java` extending `BaseMapper<NodeMetricsLog>`.
Create `NodeMetricsLogDao.java` extending `ServiceImpl` using the ClickHouse datasource configuration. (Use `@DS("clickhouse")` if using dynamic-datasource, or just rely on existing clickhouse session setup).

- [ ] **Step 2: Implement NodeService**
```java
package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.NodeMetricsHistoryDto;
import cc.riskswap.trader.admin.common.model.dto.NodeStatusDto;
import cc.riskswap.trader.admin.dao.NodeMetricsLogDao;
import cc.riskswap.trader.admin.dao.entity.NodeMetricsLog;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final NodeMetricsLogDao nodeMetricsLogDao;

    private static final String NODE_STATUS_PREFIX = "node:status:";
    private static final long OFFLINE_THRESHOLD_MS = 30000; // 30 seconds

    public List<NodeStatusDto> getAllNodes() {
        Set<String> keys = stringRedisTemplate.keys(NODE_STATUS_PREFIX + "*");
        List<NodeStatusDto> nodes = new ArrayList<>();
        if (keys == null || keys.isEmpty()) return nodes;

        long now = System.currentTimeMillis();
        for (String key : keys) {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                NodeStatusDto node = JSONUtil.toBean(json, NodeStatusDto.class);
                node.setOnline((now - node.getTimestamp()) <= OFFLINE_THRESHOLD_MS);
                nodes.add(node);
            }
        }
        return nodes;
    }

    public NodeMetricsHistoryDto getNodeHistory(String nodeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        LambdaQueryWrapper<NodeMetricsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NodeMetricsLog::getNodeId, nodeId);
        wrapper.ge(NodeMetricsLog::getTimestamp, startTime);
        wrapper.le(NodeMetricsLog::getTimestamp, endTime);
        wrapper.orderByAsc(NodeMetricsLog::getTimestamp);

        List<NodeMetricsLog> logs = nodeMetricsLogDao.list(wrapper);

        NodeMetricsHistoryDto dto = new NodeMetricsHistoryDto();
        List<String> times = new ArrayList<>();
        List<Float> cpus = new ArrayList<>();
        List<Float> mems = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        for (NodeMetricsLog log : logs) {
            times.add(log.getTimestamp().format(formatter));
            cpus.add(log.getCpuUsage());
            mems.add(log.getMemoryUsage());
        }

        dto.setTimestamps(times);
        dto.setCpuUsages(cpus);
        dto.setMemoryUsages(mems);
        return dto;
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add admin-server/src/main/java/cc/riskswap/trader/admin/dao admin-server/src/main/java/cc/riskswap/trader/admin/service
git commit -m "feat: implement node service for redis and clickhouse"
```

---

### Task 3: Implement Backend Controller

**Files:**
- Create: `admin-server/src/main/java/cc/riskswap/trader/admin/controller/NodeController.java`

- [ ] **Step 1: Create NodeController**
```java
package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.NodeMetricsHistoryDto;
import cc.riskswap.trader.admin.common.model.dto.NodeStatusDto;
import cc.riskswap.trader.admin.service.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @GetMapping
    public ResData<List<NodeStatusDto>> getAllNodes() {
        return ResData.success(nodeService.getAllNodes());
    }

    @GetMapping("/{nodeId}/history")
    public ResData<NodeMetricsHistoryDto> getNodeHistory(
            @PathVariable String nodeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return ResData.success(nodeService.getNodeHistory(nodeId, startTime, endTime));
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add admin-server/src/main/java/cc/riskswap/trader/admin/controller/NodeController.java
git commit -m "feat: add node controller"
```

---

### Task 4: Frontend Services and Routes

**Files:**
- Create: `admin-web/src/services/node.ts`
- Modify: `admin-web/src/router/index.ts`
- Modify: `admin-web/src/layout/components/Sidebar.vue` (if applicable)

- [ ] **Step 1: Create API Service**
```typescript
import http from '../utils/http';
import type { ResData } from './types';

export interface NodeStatusDto {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  ipAddress: string;
  version: string;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  timestamp: number;
  online: boolean;
}

export interface NodeMetricsHistoryDto {
  timestamps: string[];
  cpuUsages: number[];
  memoryUsages: number[];
}

export const getAllNodes = () => {
  return http.get('/nodes').then(res => res.data as ResData<NodeStatusDto[]>);
};

export const getNodeHistory = (nodeId: string, startTime: string, endTime: string) => {
  return http.get(`/nodes/${nodeId}/history`, { params: { startTime, endTime } })
    .then(res => res.data as ResData<NodeMetricsHistoryDto>);
};
```

- [ ] **Step 2: Add Route**
Update `admin-web/src/router/index.ts` to add:
```typescript
{
  path: 'node/list',
  name: 'NodeList',
  component: () => import('../pages/node/List.vue'),
  meta: { title: '节点管理', icon: 'Monitor' }
}
```

- [ ] **Step 3: Update Sidebar**
Add the menu item for Node Management in `admin-web/src/layout/components/Sidebar.vue`.

- [ ] **Step 4: Commit**
```bash
git add admin-web/src/services admin-web/src/router admin-web/src/layout
git commit -m "feat: add frontend node services and routes"
```

---

### Task 5: Frontend Node List Page

**Files:**
- Create: `admin-web/src/pages/node/List.vue`

- [ ] **Step 1: Implement List.vue**
Create a standard Vue 3 component using `<script setup lang="ts">`.
Include:
- Top overview cards: Total Online, Total Offline, High CPU Load (>80%).
- Data table (`el-table`) displaying nodes.
- Auto-refresh mechanism (e.g., `setInterval` every 10 seconds).
- History Dialog (`el-dialog`) that contains an ECharts instance showing CPU and Memory trends when a node is clicked.

- [ ] **Step 2: Test the Page**
Run the Vue app locally and verify the UI renders without errors.

- [ ] **Step 3: Commit**
```bash
git add admin-web/src/pages/node/List.vue
git commit -m "feat: implement node list and history charts"
```
