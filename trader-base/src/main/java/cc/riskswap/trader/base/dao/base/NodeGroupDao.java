package cc.riskswap.trader.base.dao.base;

import cc.riskswap.trader.base.dao.base.entity.NodeGroup;
import cc.riskswap.trader.base.dao.base.mapper.NodeGroupMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NodeGroupDao extends ServiceImpl<NodeGroupMapper, NodeGroup> {

    public List<NodeGroup> listAll() {
        LambdaQueryWrapper<NodeGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NodeGroup::getSort)
                .orderByAsc(NodeGroup::getId);
        return list(wrapper);
    }

    public NodeGroup getDefaultPendingGroup() {
        LambdaQueryWrapper<NodeGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NodeGroup::getIsDefaultPending, true)
                .last("limit 1");
        return getOne(wrapper, false);
    }

    public NodeGroup getByCode(String code) {
        LambdaQueryWrapper<NodeGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NodeGroup::getCode, code)
                .last("limit 1");
        return getOne(wrapper, false);
    }
}
