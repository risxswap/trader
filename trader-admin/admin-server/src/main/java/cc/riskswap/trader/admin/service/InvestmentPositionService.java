package cc.riskswap.trader.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.InvestmentPositionDto;
import cc.riskswap.trader.admin.common.model.query.InvestmentPositionListQuery;
import cc.riskswap.trader.base.dao.InvestmentPositionDao;
import cc.riskswap.trader.base.dao.entity.InvestmentPosition;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

import cc.riskswap.trader.admin.common.model.param.InvestmentPositionParam;
import cc.riskswap.trader.admin.common.model.param.InvestmentPositionUpdateParam;
import cc.riskswap.trader.admin.exception.Warning;
import java.time.OffsetDateTime;

@Service
public class InvestmentPositionService {

    @Autowired
    private InvestmentPositionDao investmentPositionDao;

    public void add(InvestmentPositionParam param) {
        InvestmentPosition position = new InvestmentPosition();
        BeanUtil.copyProperties(param, position);
        position.setCreatedAt(OffsetDateTime.now());
        position.setUpdatedAt(OffsetDateTime.now());
        investmentPositionDao.save(position);
    }

    public void update(InvestmentPositionUpdateParam param) {
        InvestmentPosition position = investmentPositionDao.getById(param.getId());
        if (position == null) {
            throw new Warning("持仓记录不存在");
        }
        BeanUtil.copyProperties(param, position);
        position.setUpdatedAt(OffsetDateTime.now());
        investmentPositionDao.updateById(position);
    }

    public void delete(Integer id) {
        if (!investmentPositionDao.removeById(id)) {
            throw new Warning("删除失败，记录可能不存在");
        }
    }

    public PageDto<InvestmentPositionDto> list(InvestmentPositionListQuery q) {
        cc.riskswap.trader.base.dao.query.InvestmentPositionListQuery listQuery = new cc.riskswap.trader.base.dao.query.InvestmentPositionListQuery();
        BeanUtil.copyProperties(q, listQuery);
        Page<InvestmentPosition> p = investmentPositionDao.pageQuery(listQuery);
        
        List<InvestmentPositionDto> list = p.getRecords().stream()
                .map(t -> BeanUtil.copyProperties(t, InvestmentPositionDto.class))
                .collect(Collectors.toList());
        
        PageDto<InvestmentPositionDto> res = new PageDto<>();
        res.setItems(list);
        res.setTotal(p.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }
}
