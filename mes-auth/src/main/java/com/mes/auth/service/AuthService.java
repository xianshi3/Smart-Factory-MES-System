package com.mes.auth.service;

import com.mes.auth.dto.LoginDTO;
import com.mes.auth.dto.RegisterDTO;
import com.mes.auth.dto.UpdatePasswordDTO;
import com.mes.auth.dto.UpdateUserDTO;
import com.mes.auth.entity.User;
import com.mes.auth.mapper.UserMapper;
import com.mes.common.exception.BizException;
import com.mes.common.exception.ErrorCode;
import com.mes.common.result.Result;
import com.mes.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
        
        // 验证密码 - 直接比较（支持明文和BCrypt）
        String storedPassword = user.getPassword();
        String inputPassword = dto.getPassword();
        boolean matched = false;
        
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            matched = passwordEncoder.matches(inputPassword, storedPassword);
        } else if (storedPassword != null && storedPassword.equals(inputPassword)) {
            matched = true;
        }
        
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            matched = passwordEncoder.matches(inputPassword, storedPassword);
        } else if (storedPassword != null) {
            // 临时：直接比较明文密码
            matched = storedPassword.equals(inputPassword);
            if (!matched) {
                // 再尝试BCrypt（可能是旧的hash）
                matched = passwordEncoder.matches(inputPassword, storedPassword);
            }
        }
        
        if (!matched) {
            log.warn("密码验证失败 - 输入: {}, 存储: {}", inputPassword, storedPassword);
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
        // 创建新用户 (密码BCrypt加密)
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
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

    /**
     * 更新用户个人资料
     * @param token JWT令牌
     * @param dto 更新内容
     */
    @Transactional
    public void updateUserProfile(String token, UpdateUserDTO dto) {
        Long userId = jwtUtils.getUserId(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        // 更新字段
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getDepartment() != null) {
            user.setDepartment(dto.getDepartment());
        }
        if (dto.getPosition() != null) {
            user.setPosition(dto.getPosition());
        }
        userMapper.updateById(user);
        log.info("用户 {} 更新个人资料成功", user.getUsername());
    }

    /**
     * 修改密码
     * @param token JWT令牌
     * @param dto 密码修改内容
     */
    @Transactional
    public void changePassword(String token, UpdatePasswordDTO dto) {
        Long userId = jwtUtils.getUserId(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        // 验证当前密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR.getCode(), "当前密码错误");
        }
        // 验证新密码和确认密码
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "两次输入的密码不一致");
        }
        // 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        log.info("用户 {} 修改密码成功", user.getUsername());
    }
}
