package com.streamcell.web.user.repository;

import com.streamcell.web.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserRepository {

    List<User> findAll();

    List<Long> findExistingUserIds(List<Long> userIds);

    @Select("""
        select count(1)
        from web.users
        where user_id = #{userId}
    """)
    int existsByUserId(Long userId);
}
