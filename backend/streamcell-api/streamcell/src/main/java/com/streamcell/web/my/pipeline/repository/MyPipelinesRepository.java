package com.streamcell.web.my.pipeline.repository;

import com.streamcell.web.my.pipeline.vo.MyPipeline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MyPipelinesRepository {

    @Select("""
        select
                 pipeline_id
               , owner_user_id
               , pipeline_name
               , pipeline_type
               , status
               , natural_language_request
               , pipeline_plan_json
               , generated_sql
         from platform.pipeline a
        where a.owner_user_id = #{userId}
    """)
    List<MyPipeline> findPipelinesByUserId(Long userId);
}
