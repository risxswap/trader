package cc.riskswap.trader.admin.dao;

import cc.riskswap.trader.admin.dao.entity.InvestmentLog;
import cc.riskswap.trader.admin.dao.mapper.InvestmentLogMapper;
import cc.riskswap.trader.admin.dao.query.InvestmentLogListQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import cn.hutool.core.util.StrUtil;
import java.util.List;

/**
 * 投资日志DAO
 */
@Repository
public class InvestmentLogDao extends ServiceImpl<InvestmentLogMapper, InvestmentLog> {

    public Page<InvestmentLog> pageQuery(InvestmentLogListQuery query) {
        Page<InvestmentLog> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<InvestmentLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getInvestmentId() != null) {
            wrapper.eq(InvestmentLog::getInvestmentId, query.getInvestmentId());
        }
        if (StrUtil.isNotBlank(query.getType())) {
            wrapper.eq(InvestmentLog::getType, query.getType());
        }
        wrapper.orderByDesc(InvestmentLog::getRecordDate);
        return this.page(page, wrapper);
    }

    public List<InvestmentLog> listByInvestmentIds(List<Integer> investmentIds) {
        if (investmentIds == null || investmentIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<InvestmentLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(InvestmentLog::getInvestmentId, investmentIds);
        wrapper.orderByDesc(InvestmentLog::getRecordDate);
        return this.list(wrapper);
    }

    public InvestmentLog getLatestByInvestmentId(Integer investmentId) {
        return lambdaQuery()
                .eq(InvestmentLog::getInvestmentId, investmentId)
                .orderByDesc(InvestmentLog::getRecordDate)
                .last("LIMIT 1")
                .one();
    }
}
