package com.streamcell.web.user.converter;

import com.streamcell.global.security.CustomUserDetails;
import com.streamcell.web.user.domain.User;
import com.streamcell.web.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {

    UserResponse toDTO(User user);

    CustomUserDetails customUserDetailsOf(User user);
}
