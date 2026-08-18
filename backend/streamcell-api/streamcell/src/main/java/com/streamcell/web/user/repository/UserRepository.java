package com.streamcell.web.user.repository;

import com.streamcell.web.user.domain.User;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserRepository {

    List<User> findAll();


    @Select("""
        select
                user_id,
                login_id,
                name,
                email,
                password,
                status
            from web.users
            where user_id = #{userId};
    """)
    Optional<User> findById(Long userId);

    @Select("""
        select
                user_id,
                login_id,
                name,
                email,
                password,
                status
            from web.users
            where login_id = #{LoginId};
    """)
    Optional<User> findByLoginId(String LoginId);

    @Select("""
        select
                user_id,
                name,
                email,
                password,
                status
            from web.users
            where name = #{userName};
    """)
    List<User> findByUserName(String userName);

    List<Long> findExistingUserIds(List<Long> userIds);

    @Select("""
        select count(1)
        from web.users
        where user_id = #{userId}
    """)
    int existsByUserId(Long userId);
}
