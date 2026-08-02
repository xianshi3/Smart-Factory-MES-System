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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务类
 * 处理用户登录、注册、Token验证等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String LOGIN_FAIL_KEY = "auth:fail:";
    private static final String TOKEN_BLACKLIST_KEY = "auth:blacklist:";
    /** 连续登录失败超过该次数则锁定账户 */
    private static final int LOGIN_FAIL_LIMIT = 5;
    /** 账户锁定时长（秒） */
    private static final long LOGIN_LOCK_SECONDS = 900L;

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     * @param dto 登录请求参数
     * @return 登录结果（包含Token）
     */
    public Result<Map<String, Object>> login(LoginDTO dto) {
        // 检查账户是否被锁定
        if (isAccountLocked(dto.getUsername())) {
            throw new BizException(ErrorCode.USER_LOCKED);
        }

        // 根据用户名查询用户
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            recordLoginFail(dto.getUsername());
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证密码
        String storedPassword = user.getPassword();
        String inputPassword = dto.getPassword();
        boolean matched = false;

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
            recordLoginFail(dto.getUsername());
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }
        // 登录成功，清除失败计数
        clearLoginFail(dto.getUsername());

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
     * 用户登出：将当前Token加入黑名单，使其立即失效
     * @param token JWT令牌
     */
    public void logout(String token) {
        String rawToken = token.replace("Bearer ", "");
        try {
            long ttlMillis = jwtUtils.getRemainingMillis(rawToken);
            if (ttlMillis <= 0) {
                return;
            }
            redisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_KEY + sha256(rawToken), "1",
                    Duration.ofSeconds(Math.max(1, ttlMillis / 1000)));
            log.info("Token已加入黑名单，剩余有效期 {}ms", ttlMillis);
        } catch (Exception e) {
            // Redis不可用或Token无效时降级：不影响登出响应
            log.warn("Token加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查账户是否被锁定（Redis不可用时返回false，降级为不锁定）
     */
    private boolean isAccountLocked(String username) {
        try {
            String count = redisTemplate.opsForValue().get(LOGIN_FAIL_KEY + username);
            return count != null && Integer.parseInt(count) >= LOGIN_FAIL_LIMIT;
        } catch (Exception e) {
            log.warn("查询登录失败计数失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 记录一次登录失败（首次失败时设置TTL，Redis不可用时降级跳过）
     */
    private void recordLoginFail(String username) {
        try {
            String key = LOGIN_FAIL_KEY + username;
            Boolean created = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(LOGIN_LOCK_SECONDS));
            if (Boolean.TRUE.equals(created)) {
                return;
            }
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count >= LOGIN_FAIL_LIMIT) {
                log.warn("用户 {} 登录失败次数达到 {}，账户已锁定{}分钟", username, count, LOGIN_LOCK_SECONDS / 60);
            }
        } catch (Exception e) {
            log.warn("记录登录失败次数失败: {}", e.getMessage());
        }
    }

    /**
     * 登录成功时清除失败计数（Redis不可用时降级跳过）
     */
    private void clearLoginFail(String username) {
        try {
            redisTemplate.delete(LOGIN_FAIL_KEY + username);
        } catch (Exception e) {
            log.warn("清除登录失败计数失败: {}", e.getMessage());
        }
    }

    /**
     * Token是否在黑名单中（Redis不可用时返回false，不阻断请求）
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_KEY + sha256(token)));
        } catch (Exception e) {
            log.warn("查询Token黑名单失败: {}", e.getMessage());
            return false;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
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
        // 将当前Token加入黑名单，使旧会话立即失效
        logout(token);
        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    public Map<String, Object> getSettings(String token) {
        Long userId = jwtUtils.getUserId(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND);
        Map<String, Object> settings = new HashMap<>();
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                settings = mapper.readValue(user.getAvatar(), HashMap.class);
            } catch (Exception e) {
                log.warn("读取设置失败: {}", e.getMessage());
            }
        }
        settings.putIfAbsent("compactMode", false);
        settings.putIfAbsent("animation", true);
        settings.putIfAbsent("autoRefresh", 30);
        settings.putIfAbsent("wsPush", true);
        settings.putIfAbsent("deviceAlarmNotify", true);
        settings.putIfAbsent("orderNotify", true);
        settings.putIfAbsent("qualityNotify", true);
        return settings;
    }

    @Transactional
    public void saveSettings(String token, Map<String, Object> settings) {
        Long userId = jwtUtils.getUserId(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            user.setAvatar(mapper.writeValueAsString(settings));
            userMapper.updateById(user);
            log.info("用户 {} 保存设置成功", user.getUsername());
        } catch (Exception e) {
            throw new BizException("保存设置失败: " + e.getMessage());
        }
    }
}
