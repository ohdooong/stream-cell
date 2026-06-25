package com.streamcell.platform.topic.repository;

import com.streamcell.platform.topic.vo.Topic;
import java.util.List;

import com.streamcell.platform.topic.vo.TopicPermission;
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

    @Select("""
        select
               a.permission_id
             , a.topic_id
             , tm.topic_name
             , a.user_id
             , b.name as user_name
             , a.permission_type
          from platform.topic_permission a
          join web.users b
            on a.user_id = b.user_id
          join platform.topic_metadata tm
            on a.topic_id = tm.topic_id
         where a.topic_id = #{topicId}
    """)
    List<TopicPermission> findTopicPermissions(Long topicId);

    @Select("""
        select
               a.permission_id
             , a.topic_id
             , tm.topic_name
             , a.user_id
             , b.name as user_name
             , a.permission_type
          from platform.topic_permission a
          join web.users b
            on a.user_id = b.user_id
          join platform.topic_metadata tm
            on a.topic_id = tm.topic_id
         where a.user_id = #{userId}
    """)
    List<TopicPermission> findTopicPermissionByUserId(Long userId);

    @Update("""
        merge into platform.topic_permission a
        using (
            select #{topicId} as topic_id,
                   #{userId} as user_id) b
        on a.topic_id = b.topic_id and a.user_id = b.user_id
        when matched then
            update set a.permission_type = #{permissionType}
                     , a.updated_by = 'SYSTEM'
                     , a.updated_at = now()
        when not matched then
            insert (topic_id, user_id, permission_type, created_by, created_at, updated_by, updated_at)
            values (b.topic_id, b.user_id, #{permissionType}, 'SYSTEM', now(), 'SYSTEM', now())
    """)
    int mergeIntoTopicPermission(Long topicId, Long userId);
}
