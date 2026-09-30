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
                x.user_id,
                x.login_id,
                x.name,
                x.email,
                x.password as encrypted_password,
                x.status,
                z.role_name as role
            from web.users x
            join web.user_roles y
              on x.user_id = y.user_id
            join web.roles z
              on y.role_id = z.role_id
            where x.user_id = #{userId};
    """)
    Optional<User> findById(Long userId);

    @Select("""
        select
                x.user_id,
                x.login_id,
                x.name,
                x.email,
                x.password as encrypted_password,
                x.status,
                z.role_name as role
            from web.users x
            join web.user_roles y
              on x.user_id = y.user_id
            join web.roles z
              on y.role_id = z.role_id
            where x.login_id = #{loginId};
    """)
    Optional<User> findByLoginId(String loginId);

    @Select("""
        select
                x.user_id,
                x.login_id,
                x.name,
                x.email,
                x.password as encrypted_password,
                x.status,
                z.role_name as role
            from web.users x
            join web.user_roles y
              on x.user_id = y.user_id
            join web.roles z
              on y.role_id = z.role_id
            where x.name = #{userName};
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
