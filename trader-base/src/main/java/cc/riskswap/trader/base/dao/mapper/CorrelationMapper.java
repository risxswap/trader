package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.base.dao.entity.Correlation;
import cc.riskswap.trader.base.dao.entity.CorrelationDuplicateGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 证券相关性Mapper接口
 */
@ClickHouseMapper
@Mapper
public interface CorrelationMapper extends BaseMapper<Correlation> {

    @Select("SELECT id, asset1, asset1_type, asset2, asset2_type, coefficient, p_value, period, created_at, updated_at FROM correlation WHERE id = #{id} LIMIT 1")
    Correlation selectByPrimaryId(@Param("id") Long id);

    @Select("""
            SELECT id, asset1, asset1_type, asset2, asset2_type, coefficient, p_value, period, created_at, updated_at
            FROM correlation
            WHERE asset1 = #{asset1}
              AND asset2 = #{asset2}
              AND period = #{period}
            ORDER BY updated_at DESC, id DESC
            LIMIT 1
            """)
    Correlation selectLatestByUniqueKey(@Param("asset1") String asset1,
                                        @Param("asset2") String asset2,
                                        @Param("period") String period);

    @Select("""
            SELECT count()
            FROM (
                SELECT asset1, asset2, period
                FROM correlation
                WHERE (#{asset1} = '' OR asset1 = #{asset1})
                  AND (#{asset2} = '' OR asset2 = #{asset2})
                  AND (#{period} = '' OR period = #{period})
                  AND (#{hasMinCoefficient} = 0 OR coefficient >= #{minCoefficient})
                  AND (#{hasMaxCoefficient} = 0 OR coefficient <= #{maxCoefficient})
                GROUP BY asset1, asset2, period
            ) latest
            """)
    long countLatestPage(@Param("asset1") String asset1,
                         @Param("asset2") String asset2,
                         @Param("period") String period,
                         @Param("hasMinCoefficient") int hasMinCoefficient,
                         @Param("minCoefficient") BigDecimal minCoefficient,
                         @Param("hasMaxCoefficient") int hasMaxCoefficient,
                         @Param("maxCoefficient") BigDecimal maxCoefficient);

    @Select("""
            SELECT id, asset1, asset1_type, asset2, asset2_type, coefficient, p_value, period, created_at, updated_at
            FROM (
                SELECT id, asset1, asset1_type, asset2, asset2_type, coefficient, p_value, period, created_at, updated_at
                FROM correlation
                WHERE (#{asset1} = '' OR asset1 = #{asset1})
                  AND (#{asset2} = '' OR asset2 = #{asset2})
                  AND (#{period} = '' OR period = #{period})
                  AND (#{hasMinCoefficient} = 0 OR coefficient >= #{minCoefficient})
                  AND (#{hasMaxCoefficient} = 0 OR coefficient <= #{maxCoefficient})
                ORDER BY asset1, asset2, period, updated_at DESC, id DESC
                LIMIT 1 BY asset1, asset2, period
            ) latest
            ORDER BY updated_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Correlation> selectLatestPage(@Param("asset1") String asset1,
                                       @Param("asset2") String asset2,
                                       @Param("period") String period,
                                       @Param("hasMinCoefficient") int hasMinCoefficient,
                                       @Param("minCoefficient") BigDecimal minCoefficient,
                                       @Param("hasMaxCoefficient") int hasMaxCoefficient,
                                       @Param("maxCoefficient") BigDecimal maxCoefficient,
                                       @Param("limit") long limit,
                                       @Param("offset") long offset);

    @Select("""
            SELECT asset1, asset2, period
            FROM correlation
            GROUP BY asset1, asset2, period
            HAVING count() > 1
            ORDER BY asset1, asset2, period
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CorrelationDuplicateGroup> selectDuplicateGroups(@Param("limit") int limit,
                                                          @Param("offset") int offset);

    @Select("""
            SELECT id
            FROM correlation
            WHERE asset1 = #{asset1}
              AND asset2 = #{asset2}
              AND period = #{period}
            ORDER BY updated_at DESC, id DESC
            LIMIT 100000 OFFSET 1
            """)
    List<Long> selectHistoricalIds(@Param("asset1") String asset1,
                                   @Param("asset2") String asset2,
                                   @Param("period") String period);

    @Update({
            "<script>",
            "ALTER TABLE correlation DELETE WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int deleteByIds(@Param("ids") List<Long> ids);

    @Update("ALTER TABLE correlation DELETE WHERE id = #{id}")
    int deleteByPrimaryId(@Param("id") Long id);
}
