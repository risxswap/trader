package cc.riskswap.trader.admin.dao;

import cc.riskswap.trader.admin.dao.entity.NodeGroup;
import cc.riskswap.trader.admin.dao.mapper.NodeGroupMapper;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("adminNodeGroupDao")
public class NodeGroupDao extends ServiceImpl<NodeGroupMapper, NodeGroup> {
    public List<NodeGroup> listAll() {
        return this.list();
    }

    public NodeGroup getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return lambdaQuery().eq(NodeGroup::getCode, code).last("LIMIT 1").one();
    }
}
