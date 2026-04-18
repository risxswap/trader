package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.common.model.dto.MsgPushLogDto;
import cc.riskswap.trader.base.dao.MsgPushLogDao;
import cc.riskswap.trader.base.dao.entity.MsgPushLog;
import cc.riskswap.trader.admin.exception.Warning;
import cc.riskswap.trader.admin.service.MsgPushLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;

public class MsgPushLogServiceTest {

    @Test
    void shouldReturnDetailWhenLogExists() throws Exception {
        MsgPushLogDao msgPushLogDao = Mockito.mock(MsgPushLogDao.class);
        MsgPushLogService msgPushLogService = createService(msgPushLogDao);
        MsgPushLog entity = new MsgPushLog();
        entity.setId(9);
        entity.setType("ALERT");
        entity.setTitle("日报");
        entity.setContent("# 标题");
        entity.setStatus("SUCCESS");
        entity.setChannel("WeCom");
        entity.setRecipient("ops");
        entity.setCreatedAt(OffsetDateTime.parse("2026-04-12T10:20:30+08:00"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-04-12T10:25:30+08:00"));
        Mockito.when(msgPushLogDao.getById(9)).thenReturn(entity);

        MsgPushLogDto detail = msgPushLogService.getDetail(9);

        Assertions.assertEquals(9, detail.getId());
        Assertions.assertEquals("# 标题", detail.getContent());
        Assertions.assertEquals("WeCom", detail.getChannel());
        Assertions.assertEquals(entity.getUpdatedAt(), detail.getUpdatedAt());
    }

    @Test
    void shouldThrowWarningWhenLogDetailMissing() throws Exception {
        MsgPushLogDao msgPushLogDao = Mockito.mock(MsgPushLogDao.class);
        MsgPushLogService msgPushLogService = createService(msgPushLogDao);
        Mockito.when(msgPushLogDao.getById(100)).thenReturn(null);

        Warning warning = Assertions.assertThrows(Warning.class, () -> msgPushLogService.getDetail(100));

        Assertions.assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code(), warning.getCode());
    }

    private MsgPushLogService createService(MsgPushLogDao msgPushLogDao) throws Exception {
        MsgPushLogService msgPushLogService = new MsgPushLogService();
        setField(msgPushLogService, "msgPushLogDao", msgPushLogDao);
        return msgPushLogService;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
