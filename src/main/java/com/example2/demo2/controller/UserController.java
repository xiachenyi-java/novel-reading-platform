package com.example2.demo2.controller;

import com.example2.demo2.common.Result;
import com.example2.demo2.common.UserContext;
import com.example2.demo2.common.annotation.RateLimit;
import com.example2.demo2.dto.LoginDTO;
import com.example2.demo2.dto.RefreshTokenDTO;
import com.example2.demo2.dto.UserRegisterDTO;
import com.example2.demo2.entity.User;
import com.example2.demo2.service.Iservice.IUserService;
import com.example2.demo2.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 夏辰义
 * 2026/8/2018:14
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
@Slf4j
public class UserController {

    private final IUserService iUserService;

    @Operation(summary = "用户注册")
    @RateLimit(key = "register", limit = 5, window = 60)
    @PostMapping("/register")
    public Result<User> register(@RequestBody @Valid UserRegisterDTO dto) {
        return Result.success(iUserService.register(dto));
    }

    @Operation(summary = "用户登陆")
    @RateLimit(key = "login", limit = 5, window = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {

        return Result.success(iUserService.login(loginDTO));
    }

    @Operation(
            summary = "用户登出",
            security = @SecurityRequirement(name = "BearerAuth")  // 告诉 Swagger 这个接口需要 Bearer token
    )
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Integer userId = UserContext.getUser().getUserId();
        iUserService.logout(userId, token);

        return Result.success();
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
        return Result.success(iUserService.refresh(dto.getRefreshToken()));
    }
}
