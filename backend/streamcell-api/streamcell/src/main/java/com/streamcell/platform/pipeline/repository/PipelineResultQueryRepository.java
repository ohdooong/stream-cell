package com.streamcell.platform.pipeline.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface PipelineResultQueryRepository {


    @Select("""
        select
            *
        from platform.${tableName} x
        where x.window_start >= now() - (CAST(#{rangeMinute} AS text) || ' minutes')::INTERVAL
        order by x.window_start desc
        limit #{limit}
    """)
    List<Map<String, Object>> findRowsByTableName(String tableName, Integer limit, Integer rangeMinute);


}
