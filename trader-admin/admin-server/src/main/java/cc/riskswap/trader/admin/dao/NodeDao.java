package cc.riskswap.trader.admin.dao;

import cc.riskswap.trader.admin.dao.entity.Node;
import cc.riskswap.trader.admin.dao.mapper.NodeMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("adminNodeDao")
public class NodeDao extends ServiceImpl<NodeMapper, Node> {
    public List<Node> listAll() {
        return this.list();
    }

    public long countByNodeGroupId(Long nodeGroupId) {
        return lambdaQuery().eq(Node::getNodeGroupId, nodeGroupId).count();
    }
}
