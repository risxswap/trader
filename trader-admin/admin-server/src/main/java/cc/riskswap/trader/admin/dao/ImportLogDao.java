package cc.riskswap.trader.admin.dao;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.admin.dao.entity.ImportLog;
import cc.riskswap.trader.admin.dao.mapper.ImportLogMapper;

@Repository
public class ImportLogDao extends ServiceImpl<ImportLogMapper, ImportLog>{


    /**
     * 根据文件查询是否已导入过
     * @param file
     * @return
     */
    public boolean existsByFile(String file) {
        LambdaQueryWrapper<ImportLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImportLog::getFile, file);
        queryWrapper.eq(ImportLog::getStatus, "success");
        return exists(queryWrapper);
    }
}
