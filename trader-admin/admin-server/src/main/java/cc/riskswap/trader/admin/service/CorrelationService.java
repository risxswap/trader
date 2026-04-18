package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.CorrelationDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.CorrelationParam;
import cc.riskswap.trader.admin.common.model.query.CorrelationQuery;
import cc.riskswap.trader.base.dao.CorrelationDao;
import cc.riskswap.trader.base.dao.entity.Correlation;
import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.exception.Warning;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 证券相关性服务
 */
@Slf4j
@Service
public class CorrelationService {

    @Autowired
    private CorrelationDao correlationDao;

    public PageDto<CorrelationDto> list(CorrelationQuery query) {
        cc.riskswap.trader.base.dao.query.CorrelationListQuery listQuery = BeanUtil.copyProperties(query, cc.riskswap.trader.base.dao.query.CorrelationListQuery.class);
        Page<Correlation> page = correlationDao.pageQuery(listQuery);

        PageDto<CorrelationDto> result = new PageDto<>();
        result.setTotal(page.getTotal());
        result.setPageNo((int) page.getCurrent()); 
        result.setPageSize((int) page.getSize());

        List<CorrelationDto> items = page.getRecords().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        result.setItems(items);

        return result;
    }

    public CorrelationDto detail(Long id) {
        Correlation correlation = correlationDao.getById(id.intValue());
        if (correlation == null) {
            throw new Warning(ErrorCode.NOT_FOUND.code(), "相关统计不存在");
        }
        return toDto(correlation);
    }
    
    public void add(CorrelationParam param) {
        Correlation correlation = toEntity(param);
        correlationDao.save(correlation);
    }

    public void update(CorrelationParam param) {
        if (param.getId() == null) {
            throw new IllegalArgumentException("ID cannot be null for update");
        }
        Correlation existing = correlationDao.getById(param.getId().intValue());
        if (existing == null) {
            throw new Warning(ErrorCode.NOT_FOUND.code(), "相关统计不存在");
        }

        Correlation correlation = toEntity(param);
        appendCorrelation(correlation);
    }

    public void delete(Long id) {
        correlationDao.removeById(id.intValue());
    }

    private void appendCorrelation(Correlation correlation) {
        OffsetDateTime now = OffsetDateTime.now();
        correlation.setId(null);
        correlation.setCreatedAt(now);
        correlation.setUpdatedAt(now);
        correlationDao.save(correlation);
    }

    private CorrelationDto toDto(Correlation correlation) {
        CorrelationDto dto = BeanUtil.copyProperties(correlation, CorrelationDto.class);
        dto.setAsset1(correlation.getSymbol1());
        dto.setAsset1Type(correlation.getSymbol1Type());
        dto.setAsset2(correlation.getSymbol2());
        dto.setAsset2Type(correlation.getSymbol2Type());
        return dto;
    }

    private Correlation toEntity(CorrelationParam param) {
        Correlation correlation = new Correlation();
        correlation.setCoefficient(param.getCoefficient());
        correlation.setPValue(param.getPValue());
        correlation.setPeriod(param.getPeriod());
        correlation.setSymbol1(param.getAsset1());
        correlation.setSymbol1Type(param.getAsset1Type());
        correlation.setSymbol2(param.getAsset2());
        correlation.setSymbol2Type(param.getAsset2Type());
        return correlation;
    }
}
