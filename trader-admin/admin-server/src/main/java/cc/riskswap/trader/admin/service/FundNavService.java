package cc.riskswap.trader.admin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.admin.common.model.dto.FundNavDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.query.FundNavListQuery;
import cc.riskswap.trader.base.dao.FundAdjDao;
import cc.riskswap.trader.base.dao.FundNavDao;
import cc.riskswap.trader.base.dao.entity.FundAdj;
import cc.riskswap.trader.base.dao.entity.FundNav;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FundNavService {

    @Autowired
    private FundNavDao fundNavDao;
    
    @Autowired
    private FundAdjDao fundAdjDao;

    @SuppressWarnings("null")
    public PageDto<FundNavDto> listNav(FundNavListQuery q) {
        cc.riskswap.trader.base.dao.query.FundNavListQuery listQuery = new cc.riskswap.trader.base.dao.query.FundNavListQuery();
        BeanUtils.copyProperties(q, listQuery);
        Page<FundNav> page = fundNavDao.pageQuery(listQuery);
        
        // Fetch adj factors if symbol is present
        TreeMap<LocalDate, BigDecimal> adjMap = new TreeMap<>();
        if (StrUtil.isNotBlank(q.getCode())) {
            List<FundAdj> adjs = fundAdjDao.lambdaQuery()
                    .eq(FundAdj::getCode, q.getCode())
                    .orderByAsc(FundAdj::getTime)
                    .list();
            for (FundAdj adj : adjs) {
                if (adj.getTime() != null && adj.getAdjFactor() != null) {
                    adjMap.put(adj.getTime().toLocalDate(), BigDecimal.valueOf(adj.getAdjFactor()));
                }
            }
        }
        
        List<FundNavDto> dtos = page.getRecords().stream().map(record -> {
            FundNavDto dto = new FundNavDto();
            BeanUtils.copyProperties(record, dto);
            
            // Recalculate accumNav if accumDiv is present
            if (record.getUnitNav() != null && record.getAccumDiv() != null) {
                dto.setAccumNav(record.getUnitNav().add(record.getAccumDiv()));
            }
            
            // Recalculate adjNav using adj factors
            if (record.getUnitNav() != null && !adjMap.isEmpty() && record.getTime() != null) {
                LocalDate date = record.getTime().toLocalDate();
                Map.Entry<LocalDate, BigDecimal> entry = adjMap.floorEntry(date);
                if (entry != null) {
                    dto.setAdjNav(record.getUnitNav().multiply(entry.getValue()));
                } else {
                    // If no factor found before this date, assume 1.0 or use first available? 
                    // Usually assume 1.0 if before any split
                    // But if the list of adjs starts later, maybe we should use the first one?
                    // Standard practice: if before first adj date, factor is 1.0 (or the initial factor if not 1).
                    // Let's assume 1.0
                }
            }
            
            return dto;
        }).collect(Collectors.toList());
        
        PageDto<FundNavDto> res = new PageDto<>();
        res.setTotal(page.getTotal());
        res.setItems(dtos);
        return res;
    }
}
