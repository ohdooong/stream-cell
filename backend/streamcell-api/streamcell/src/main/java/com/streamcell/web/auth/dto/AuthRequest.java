package com.streamcell.web.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AuthRequest {



    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(staticName = "from")
    @AllArgsConstructor(staticName = "from")
    public static class Login {

        private String loginId;
        private String password;

    }
}
