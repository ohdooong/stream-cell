package com.streamcell.web.user.adapter;

import com.streamcell.platform._common.port.UserLookupPort;
import com.streamcell.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookupPort {

    private final UserRepository repository;

    @Override
    public List<Long> findExistingUserIds(List<Long> userIds) {
        return repository.findExistingUserIds(userIds);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        int exists = repository.existsByUserId(userId);
        return exists > 0;
    }
}
