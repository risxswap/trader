package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.Investment;
import cc.riskswap.trader.executor.dao.mapper.InvestmentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Repository
public class InvestmentDao extends ServiceImpl<InvestmentMapper, Investment> {

    public List<Investment> listByStatusAndExecutorId(String status, String executorId) {
        // Implement executorId logic if needed, otherwise just list by status
        LambdaQueryWrapper<Investment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Investment::getStatus, status);
        return this.list(wrapper);
    }

    public List<Investment> listByStatus(Set<String> statuses) {
        LambdaQueryWrapper<Investment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Investment::getStatus, statuses);
        return this.list(wrapper);
    }

    public void updateStatusAndExecutorId(Long id, String status, String executorId) {
        Investment investment = new Investment();
        investment.setId(id);
        investment.setStatus(status);
        this.updateById(investment);
    }
}