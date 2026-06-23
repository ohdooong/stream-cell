package com.streamcell.platform.topic.repository;

import com.streamcell.platform.topic.vo.Topic;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface TopicRepository {

    @Select("""
        select
            topic_id,
            topic_name,
            display_name,
            description,
            schema_json,
            time_field,
            message_format
        from platform.topic_metadata
    """)
    List<Topic> findAll();

    @Insert("""
        merge into platform.topic_metadata as t
            using (
                select
                    #{topic} as topic_name
            ) as x
        on (t.topic_name = x.topic_name)
        when matched then
            update set
                topic_name = #{topic},
                updated_at = now()
        when not matched then
            insert (topic_name, created_by, created_at, updated_by, updated_at)
            values (#{topic}, 'ADMIN', now(), 'ADMIN', now())
    """)
    int mergeIntoTopic(String topic);

    @Select("""
        select
            topic_id,
            topic_name,
            display_name,
            description,
            schema_json,
            time_field,
            message_format
        from platform.topic_metadata
       where topic_id = #{topicId}
    """)
    Topic findById(Long topicId);

    @Update("""
        update platform.topic_metadata
           set display_name = #{displayName}
             , description = #{description}
             , message_format = #{description}
             , time_field = #{timeField}
             , schema_json = #{schemaJson}::jsonb
         where topic_id = #{topicId}
    """)
    int updateTopicSchema(Topic topic);
}
