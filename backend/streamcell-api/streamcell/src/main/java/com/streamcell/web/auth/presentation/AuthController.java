package com.streamcell.web.auth.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.web.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/web/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "사용자 로그인 요청", description = "사용자 로그인 요청을 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<?>> loginRequest() {
        return ResponseEntity.ok(BaseResponse.success(null));
    }

}
