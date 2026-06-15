package com.streamcell.web.user.repository;

import com.streamcell.web.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserRepository {

    List<User> findAll();

}
