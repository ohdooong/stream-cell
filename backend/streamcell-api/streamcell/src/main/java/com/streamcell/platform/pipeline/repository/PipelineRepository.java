package com.streamcell.platform.pipeline.repository;

import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
               , description
               , pipeline_type
               , status as pipeline_status
               , natural_language_request
               , pipeline_plan_json
               , generated_sql
         from platform.pipeline a
        where a.pipeline_id = #{pipelineId}
     """)
    Optional<Pipeline> findPipelineByPipelineId(Long pipelineId);

    @Insert("""
        insert into platform.pipeline_artifact
        (
            pipeline_id,
            artifact_type,
            original_file_name,
            stored_file_name,
            stored_file_path,
            flink_jar_id,
            created_by,
            created_at,
            updated_by,
            updated_at
        )
        values (
                #{pipelineId},
                #{artifactType},
                #{originalFileName},
                #{storedFileName},
                #{storedFilePath},
                #{flinkJarId},
                'ADMIN',
                now(),
                'ADMIN',
                now()
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "artifactId")
    int insertPipelineArtifact(PipelineArtifact artifact);

    @Insert("""
        insert into platform.custom_job_config
        (
            pipeline_id,
            entry_class,
            input_topics,
            output_topics,
            program_args,
            created_by,
            created_at,
            updated_by,
            updated_at
        )
        values (
                #{pipelineId},
                #{entryClass},
                #{inputTopics}::json,
                #{outputTopics}::json,
                #{programArgs}::json,
                'ADMIN',
                now(),
                'ADMIN',
                now()
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "configId")
    int insertCustomJobConfig(CustomJobConfig customJobConfig);
}
