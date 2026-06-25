package com.streamcell.platform._common.port;


import java.util.List;

public interface UserLookupPort {
    List<Long> findExistingUserIds(List<Long> userIds);

    boolean existsByUserId(Long userId);
}
