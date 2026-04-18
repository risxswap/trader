package cc.riskswap.trader.admin.dao.base.mapper;

import cc.riskswap.trader.admin.dao.base.entity.NodeMonitor;
import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

@ClickHouseMapper
public interface NodeMonitorMapper extends BaseMapper<NodeMonitor> {

    @Select("""
            SELECT
              collected_at AS timestamp,
              toFloat32(cpu_load) AS cpu_usage,
              toFloat32(
                if(physical_memory_total <= 0, 0, (physical_memory_total - physical_memory_available) / physical_memory_total)
              ) AS memory_usage
            FROM node_monitor
            WHERE node_id = #{nodeId}
              AND collected_at >= #{startTime}
              AND collected_at <= #{endTime}
            ORDER BY collected_at
            """)
    List<NodeMonitor> listHistory(
            @Param("nodeId") String nodeId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}
