package com.streamcell.platform.pipeline.repository;

import com.streamcell.platform.pipeline.vo.Pipeline;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface PipelineRepository {

    @Insert("""
        insert into platform.pipeline
        (
             owner_user_id
           , pipeline_name
           , description
           , pipeline_type
           , status
           , created_by
           , created_at
           , updated_by
           , updated_at
        )
        values
        (
            #{ownerUserId}
          , #{pipelineName}
          , #{description}
          , #{pipelineType}
          , #{pipelineStatus}
          , 'ADMIN'
          , now()
          , 'ADMIN'
          , now()
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "pipelineId")
    int insert(Pipeline pipeline);

    @Update("""
        update platform.pipeline
           set 
               pipeline_name = #{pipelineName}
             , description = #{description}
             , pipeline_type = #{pipelineType}
             , updated_by = 'ADMIN'
             , updated_at = now()
        where pipeline_id = #{pipelineId}
    """)
    int update(Pipeline pipeline);

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
        where a.pipeline_id = #{pipelineId}
     """)
    Pipeline findPipelineByPipelineId(Long pipelineId);


}
