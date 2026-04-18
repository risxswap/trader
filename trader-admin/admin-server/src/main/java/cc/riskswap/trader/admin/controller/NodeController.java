package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.NodeGroupDto;
import cc.riskswap.trader.admin.common.model.dto.NodeMetricsHistoryDto;
import cc.riskswap.trader.admin.common.model.dto.NodeStatusDto;
import cc.riskswap.trader.admin.common.model.param.NodeApproveParam;
import cc.riskswap.trader.admin.common.model.param.NodeGroupParam;
import cc.riskswap.trader.admin.common.model.param.NodeParam;
import jakarta.validation.Valid;
import cc.riskswap.trader.admin.service.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/node")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @GetMapping("/list")
    public ResData<List<NodeStatusDto>> getAllNodes() {
        return ResData.success(nodeService.getAllNodes());
    }

    @GetMapping("/group/list")
    public ResData<List<NodeGroupDto>> getGroups() {
        return ResData.success(nodeService.getGroups());
    }

    @PostMapping("/group")
    public ResData<Void> addGroup(@Valid @RequestBody NodeGroupParam param) {
        nodeService.addGroup(param);
        return ResData.success();
    }

    @PutMapping("/group")
    public ResData<Void> updateGroup(@Valid @RequestBody NodeGroupParam param) {
        nodeService.updateGroup(param);
        return ResData.success();
    }

    @DeleteMapping("/group/{id}")
    public ResData<Void> deleteGroup(@PathVariable Long id) {
        nodeService.deleteGroup(id);
        return ResData.success();
    }

    @GetMapping("/{id}")
    public ResData<NodeStatusDto> getNode(@PathVariable Long id) {
        return ResData.success(nodeService.getNode(id));
    }

    @PostMapping("/approve")
    public ResData<Void> approve(@Valid @RequestBody NodeApproveParam param) {
        nodeService.approve(param);
        return ResData.success();
    }

    @PutMapping
    public ResData<Void> update(@Valid @RequestBody NodeParam param) {
        nodeService.update(param);
        return ResData.success();
    }

    @DeleteMapping("/{id}")
    public ResData<Void> delete(@PathVariable Long id) {
        nodeService.delete(id);
        return ResData.success();
    }

    @GetMapping("/{nodeId}/history")
    public ResData<NodeMetricsHistoryDto> getNodeHistory(
            @PathVariable String nodeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return ResData.success(nodeService.getNodeHistory(nodeId, startTime, endTime));
    }
}
