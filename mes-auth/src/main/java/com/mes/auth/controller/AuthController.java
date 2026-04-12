package com.mes.auth.controller;

import com.mes.auth.dto.LoginDTO;
import com.mes.auth.dto.RegisterDTO;
import com.mes.auth.dto.UpdatePasswordDTO;
import com.mes.auth.dto.UpdateUserDTO;
import com.mes.auth.service.AuthService;
import com.mes.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注册、获取用户信息等接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、注册、Token管理接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<Object> info(@RequestHeader("Authorization") String token) {
        return Result.ok(authService.getUserInfoFromToken(token));
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<Void> updateProfile(@RequestHeader("Authorization") String token, 
                                     @Valid @RequestBody UpdateUserDTO dto) {
        authService.updateUserProfile(token, dto);
        return Result.ok();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@RequestHeader("Authorization") String token,
                                       @Valid @RequestBody UpdatePasswordDTO dto) {
        authService.changePassword(token, dto);
        return Result.ok();
    }
}
