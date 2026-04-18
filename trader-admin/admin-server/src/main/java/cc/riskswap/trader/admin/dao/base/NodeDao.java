package cc.riskswap.trader.admin.dao.base;

import cc.riskswap.trader.admin.dao.base.entity.Node;
import cc.riskswap.trader.admin.dao.base.mapper.NodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NodeDao extends ServiceImpl<NodeMapper, Node> {

    public Node getByNodeId(String nodeId) {
        LambdaQueryWrapper<Node> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Node::getNodeId, nodeId).last("limit 1");
        return getOne(wrapper, false);
    }

    public List<Node> listAll() {
        LambdaQueryWrapper<Node> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Node::getNodeGroupId)
                .orderByDesc(Node::getUpdatedAt)
                .orderByAsc(Node::getNodeId);
        return list(wrapper);
    }

    public long countByNodeGroupId(Long nodeGroupId) {
        LambdaQueryWrapper<Node> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Node::getNodeGroupId, nodeGroupId);
        return count(wrapper);
    }
}
