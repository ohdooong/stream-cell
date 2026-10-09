package com.streamcell.web.my.topic.repository;


import com.streamcell.platform.topic.vo.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MyTopicRepository {

    @Select("""
        select distinct
            b.topic_id,
            b.topic_name,
            b.display_name,
            b.description,
            b.schema_json,
            b.time_field,
            b.message_format
        from platform.topic_permission a
        join platform.topic_metadata b
          on a.topic_id = b.topic_id
       where a.user_id = #{userId}
    """)
    List<Topic> findTopicByUserId(Long userId);
}
