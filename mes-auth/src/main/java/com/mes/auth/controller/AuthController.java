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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、注册、Token管理接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<Object> info(@RequestHeader("Authorization") String token) {
        return Result.ok(authService.getUserInfoFromToken(token));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<Void> updateProfile(@RequestHeader("Authorization") String token,
                                     @Valid @RequestBody UpdateUserDTO dto) {
        authService.updateUserProfile(token, dto);
        return Result.ok();
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@RequestHeader("Authorization") String token,
                                       @Valid @RequestBody UpdatePasswordDTO dto) {
        authService.changePassword(token, dto);
        return Result.ok();
    }

    @GetMapping("/settings")
    @Operation(summary = "获取用户设置")
    public Result<Map<String, Object>> getSettings(@RequestHeader("Authorization") String token) {
        return Result.ok(authService.getSettings(token));
    }

    @PutMapping("/settings")
    @Operation(summary = "保存用户设置")
    public Result<Void> saveSettings(@RequestHeader("Authorization") String token,
                                     @RequestBody Map<String, Object> settings) {
        authService.saveSettings(token, settings);
        return Result.ok();
    }
}
