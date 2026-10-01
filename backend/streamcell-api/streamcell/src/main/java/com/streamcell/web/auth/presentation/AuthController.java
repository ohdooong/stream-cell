package com.streamcell.web.auth.presentation;

import com.streamcell.global._common.dto.BaseResponse;
import com.streamcell.web.auth.dto.AuthRequest;
import com.streamcell.web.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Web Auth API", description = "Auth API 컨트롤러")
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
    public ResponseEntity<BaseResponse<?>> login(
        @RequestBody AuthRequest.Login loginRequest) {
        return ResponseEntity.ok(BaseResponse.success(authService.login(loginRequest)));
    }

    @Operation(summary = "사용자 로그아웃 요청", description = "사용자 로그아웃 요청을 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "로그아웃 성공 (응답없음)"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error."),
    })
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(BaseResponse.success(204, "로그아웃 성공", null));
    }
}
