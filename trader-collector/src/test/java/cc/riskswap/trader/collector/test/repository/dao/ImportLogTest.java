package cc.riskswap.trader.collector.test.repository.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.common.enums.ImportLogTypeEnum;
import cc.riskswap.trader.base.dao.ImportLogDao;
import cc.riskswap.trader.base.dao.entity.ImportLog;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ImportLogTest {

    @Autowired
    private ImportLogDao importLogDao;

    @Test
    public void testInsert() {
        ImportLog importLog = new ImportLog();
        importLog.setFile("test.csv");
        importLog.setStatus("success");
        importLog.setRemark("test");
        importLog.setType(ImportLogTypeEnum.FILE.code);
        importLogDao.save(importLog);
    }

    @Test
    public void testGetByFile() {
        Boolean exist = importLogDao.existsByFile("test.csv");
        log.info("importLog: {}", exist);
    }
}
