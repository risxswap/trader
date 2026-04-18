package cc.riskswap.trader.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.admin.common.model.dto.ExchangeDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.query.ExchangeListQuery;
import cc.riskswap.trader.admin.dao.ExchangeDao;
import cc.riskswap.trader.admin.dao.entity.Exchange;
import cn.hutool.core.bean.BeanUtil;

@Service
public class ExchangeService {

    @Autowired
    private ExchangeDao exchangeDao;

    public PageDto<ExchangeDto> list(ExchangeListQuery q) {
        cc.riskswap.trader.admin.dao.query.ExchangeListQuery listQuery = new cc.riskswap.trader.admin.dao.query.ExchangeListQuery();
        BeanUtil.copyProperties(q, listQuery);
        Page<Exchange> page = exchangeDao.pageQuery(listQuery);
        List<ExchangeDto> list = page.getRecords().stream()
                .map(e -> BeanUtil.copyProperties(e, ExchangeDto.class))
                .collect(Collectors.toList());
        
        PageDto<ExchangeDto> res = new PageDto<>();
        res.setItems(list);
        res.setTotal(page.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }
}
