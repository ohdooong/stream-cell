package com.streamcell.global.security.utils;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static String getUserLoginId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
         && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getLoginId();
        }

        return null;
    }

    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        throw new BaseAPIException(ErrorCode.NOT_FOUND_USER);
    }
}
