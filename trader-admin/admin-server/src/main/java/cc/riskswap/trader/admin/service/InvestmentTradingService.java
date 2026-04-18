package cc.riskswap.trader.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.InvestmentTradingDto;
import cc.riskswap.trader.admin.common.model.query.InvestmentTradingListQuery;
import cc.riskswap.trader.admin.dao.InvestmentTradingDao;
import cc.riskswap.trader.admin.dao.entity.InvestmentTrading;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class InvestmentTradingService {

    @Autowired
    private InvestmentTradingDao investmentTradingDao;

    public PageDto<InvestmentTradingDto> list(InvestmentTradingListQuery q) {
        cc.riskswap.trader.admin.dao.query.InvestmentTradingListQuery listQuery = new cc.riskswap.trader.admin.dao.query.InvestmentTradingListQuery();
        BeanUtil.copyProperties(q, listQuery);
        Page<InvestmentTrading> p = investmentTradingDao.pageQuery(listQuery);
        
        List<InvestmentTradingDto> list = p.getRecords().stream()
                .map(t -> BeanUtil.copyProperties(t, InvestmentTradingDto.class))
                .collect(Collectors.toList());
        
        PageDto<InvestmentTradingDto> res = new PageDto<>();
        res.setItems(list);
        res.setTotal(p.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }
}
