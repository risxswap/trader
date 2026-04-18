package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.Investment;
import cc.riskswap.trader.base.dao.mapper.InvestmentMapper;
import cc.riskswap.trader.base.dao.query.InvestmentListQuery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import cn.hutool.core.util.StrUtil;

/**
 * 投资DAO
 */
@Repository
public class InvestmentDao extends ServiceImpl<InvestmentMapper, Investment> {

    public Page<Investment> pageQuery(InvestmentListQuery query) {
        Page<Investment> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<Investment> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getName())) {
            wrapper.like(Investment::getName, query.getName());
        }
        if (StrUtil.isNotBlank(query.getGroupName())) {
            wrapper.eq(Investment::getGroupName, query.getGroupName());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(Investment::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getStrategy())) {
            wrapper.eq(Investment::getStrategy, query.getStrategy());
        }
        if (query.getBudget() != null) {
            wrapper.eq(Investment::getBudget, query.getBudget());
        }
        wrapper.orderByDesc(Investment::getCreatedAt);
        return this.page(page, wrapper);
    }

    /**
     * 根据状态查询投资列表
     */
    public List<Investment> listByStatus(Set<String> status) {
        if (CollectionUtils.isEmpty(status)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Investment> queryWrapper = new LambdaQueryWrapper<>();
        if (status.size() == 1) {
            queryWrapper.eq(Investment::getStatus, status.iterator().next());
        } else {
            queryWrapper.in(Investment::getStatus, status);
        }
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 更新投资状态
     */
    public boolean updateStatus(Integer investmentId, String status) {
        Investment investment = new Investment();
        investment.setId(investmentId);
        investment.setStatus(status);
        return updateById(investment);
    }

    /**
     * 更新投资状态和执行器ID
     */
    public boolean updateStatusAndExecutorId(Integer investmentId, String status, String executorId) {
        return lambdaUpdate()
                .eq(Investment::getId, investmentId)
                .set(Investment::getStatus, status)
                .set(Investment::getExecutorId, executorId)
                .update();
    }

    /**
     * 根据状态和执行器ID查询
     */
    public List<Investment> listByStatusAndExecutorId(String status, String executorId) {
        return lambdaQuery()
                .eq(Investment::getStatus, status)
                .eq(Investment::getExecutorId, executorId)
                .list();
    }
}
