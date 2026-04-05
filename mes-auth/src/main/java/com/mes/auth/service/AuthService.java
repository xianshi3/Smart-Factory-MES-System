package com.mes.auth.service;

import com.mes.auth.dto.LoginDTO;
import com.mes.auth.dto.RegisterDTO;
import com.mes.auth.entity.User;
import com.mes.auth.mapper.UserMapper;
import com.mes.common.exception.BizException;
import com.mes.common.exception.ErrorCode;
import com.mes.common.result.Result;
import com.mes.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务类
 * 处理用户登录、注册、Token验证等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    /**
     * 用户登录
     * @param dto 登录请求参数
     * @return 登录结果（包含Token）
     */
    public Result<Map<String, Object>> login(LoginDTO dto) {
        // 根据用户名查询用户
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        // 验证密码 - 直接比较
        if (!dto.getPassword().equals(user.getPassword())) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }
        // 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), claims);
        
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return Result.ok(result);
    }

    /**
     * 用户注册
     * @param dto 注册请求参数
     */
    public void register(RegisterDTO dto) {
        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(dto.getUsername());
        if (existUser != null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "用户名已存在");
        }
        // 创建新用户 (密码直接存储)
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        user.setRole("USER");
        userMapper.insert(user);
    }

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    public User getUserInfo(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 根据Token获取用户信息
     * @param token JWT令牌
     * @return 用户信息
     */
    public User getUserInfoFromToken(String token) {
        try {
            // 解析Token获取用户ID
            Long userId = jwtUtils.getUserId(token.replace("Bearer ", ""));
            return userMapper.selectById(userId);
        } catch (Exception e) {
            log.error("解析Token失败", e);
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }
}
