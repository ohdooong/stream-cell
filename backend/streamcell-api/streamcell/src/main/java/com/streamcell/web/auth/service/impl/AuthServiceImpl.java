package com.streamcell.web.auth.service.impl;

import com.streamcell.web.auth.service.AuthService;
import com.streamcell.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository repository;






}
