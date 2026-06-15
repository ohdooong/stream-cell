package com.streamcell.platform.topic.repository;

import com.streamcell.platform.topic.vo.Topic;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
