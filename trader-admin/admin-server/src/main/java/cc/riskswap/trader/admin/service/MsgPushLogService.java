package cc.riskswap.trader.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.admin.channel.MatrixChannel;
import cc.riskswap.trader.admin.channel.WeComChannel;
import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.common.model.dto.MsgPushLogDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.MsgPushParam;
import cc.riskswap.trader.admin.common.model.query.MsgPushLogQuery;
import cc.riskswap.trader.admin.dao.MsgPushLogDao;
import cc.riskswap.trader.admin.dao.entity.MsgPushLog;
import cc.riskswap.trader.admin.exception.Warning;
import jakarta.annotation.Resource;
import cn.hutool.core.bean.BeanUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MsgPushLogService {

    @Resource(name = "adminMsgPushLogDao")
    private MsgPushLogDao msgPushLogDao;

    @Resource
    private WeComChannel weComChannel;

    @Resource
    private MatrixChannel matrixChannel;

    public PageDto<MsgPushLogDto> list(MsgPushLogQuery query) {
        cc.riskswap.trader.admin.dao.query.MsgPushLogListQuery listQuery = new cc.riskswap.trader.admin.dao.query.MsgPushLogListQuery();
        BeanUtil.copyProperties(query, listQuery);
        Page<MsgPushLog> page = msgPushLogDao.pageQuery(listQuery);

        PageDto<MsgPushLogDto> result = new PageDto<>();
        result.setTotal(page.getTotal());
        result.setPageNo((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        
        List<MsgPushLogDto> items = page.getRecords().stream()
            .map(item -> BeanUtil.copyProperties(item, MsgPushLogDto.class))
            .collect(Collectors.toList());
        result.setItems(items);
        
        return result;
    }

    public MsgPushLogDto getDetail(Integer id) {
        MsgPushLog detail = msgPushLogDao.getById(id);
        if (detail == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "消息日志不存在");
        }
        return BeanUtil.copyProperties(detail, MsgPushLogDto.class);
    }

    public void send(MsgPushParam param) {
        String channel = param.getChannel();
        if (!StringUtils.hasText(channel)) {
            channel = "WeCom";
        }
        if ("WeCom".equalsIgnoreCase(channel) || "wecom".equalsIgnoreCase(channel)) {
            weComChannel.pushText(param.getType(), param.getTitle(), param.getContent());
            return;
        }
        if ("Matrix".equalsIgnoreCase(channel) || "matrix".equalsIgnoreCase(channel)) {
            matrixChannel.pushText(param.getType(), param.getTitle(), param.getContent());
            return;
        }
        throw new Warning(ErrorCode.BAD_REQUEST.code(), "不支持的渠道: " + channel);
    }
}
