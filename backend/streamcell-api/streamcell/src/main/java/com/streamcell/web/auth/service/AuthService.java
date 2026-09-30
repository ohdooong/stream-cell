package com.streamcell.web.auth.service;

import com.streamcell.web.auth.dto.AuthRequest;
import com.streamcell.web.auth.dto.AuthResponse;

public interface AuthService {

    AuthResponse.Token login(AuthRequest.Login loginRequest);

}
