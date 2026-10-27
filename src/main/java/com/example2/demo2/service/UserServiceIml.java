package com.example2.demo2.service;

import com.example2.demo2.common.JwtUtil;
import com.example2.demo2.common.UserContext;
import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.dto.LoginDTO;
import com.example2.demo2.dto.UserContextDTO;
import com.example2.demo2.dto.UserRegisterDTO;
import com.example2.demo2.entity.User;
import com.example2.demo2.repository.ContentRepository;
import com.example2.demo2.repository.ContentSpecifications;
import com.example2.demo2.repository.UserRepository;
import com.example2.demo2.service.Iservice.IUserService;
import com.example2.demo2.vo.AuthorProfileVO;
import com.example2.demo2.vo.LoginVO;
import com.example2.demo2.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 夏辰义
 * 2026/8/20 15:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceIml implements IUserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ContentRepository  contentRepository;


    // ========== 1. 登录 ==========
    @Override
    public LoginVO login(LoginDTO dto) {
        // 账号是否存在
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // 密码是否正确
        if (!(bCryptPasswordEncoder.matches(dto.getPassword(), user.getPasswordHash()))) {
            throw new BusinessException("用户名或密码错误");
        }

        // 生成 Access Token
        String Accesstoken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 生成 Refresh Token
        String refreshToken = UUID.randomUUID().toString();

        // 正向索引：refreshToken -> userId（刷新时查是哪个用户）
        stringRedisTemplate.opsForValue().set(
                "refresh_token:" + refreshToken,
                String.valueOf(user.getId()),
                7, TimeUnit.DAYS
        );

        // 反向索引：userId -> refreshToken（登出时查要删哪个）
        stringRedisTemplate.opsForValue().set(
                "user_refresh:" + user.getId(),
                refreshToken,
                7, TimeUnit.DAYS
        );

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(Accesstoken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserInfo(UserVO.from(user));

        return loginVO;
    }

    // ========== 2. 注册 ==========
    @Override
    public UserVO register(UserRegisterDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        log.info("注册用户: username={}", dto.getUsername());

        // 创建实体
        User user = new User();
        user.setRole("USER");
        user.setUsername(dto.getUsername());

        // 密码加密存储
        String hashed = bCryptPasswordEncoder.encode(dto.getPassword());
        user.setPasswordHash(hashed);

        userRepository.save(user);

        return UserVO.from(user);
    }

    // ========== 3. 获取当前用户 ==========
    @Override
    public UserVO getCurrentUser() {
        // 1. 获取 ThreadLocal 中的用户
        UserContextDTO user = UserContext.getUser();
        if (user.getUserId() == null) {
            throw new BusinessException("用户未登录");
        }
        long userId = user.getUserId();

        // 2. 查 Redis 缓存
        String key = "user:info:" + userId;
        String userJson = stringRedisTemplate.opsForValue().get(key);
        if (userJson != null) {
            // 缓存命中：JSON 反序列化后直接返回
            try {
                return objectMapper.readValue(userJson, UserVO.class);
            } catch (JacksonException e) {
                log.error("Redis 缓存用户数据解析失败, userId={}, json={}", userId, userJson, e);
                stringRedisTemplate.delete(key);
            }
        }

        // 3. 缓存未命中：查数据库
        User dbUser = userRepository.findById(user.getUserId()).orElseThrow(()
                -> new BusinessException("用户不存在"));

        // 4. 写入 Redis（设置过期时间，防止永久驻留）
        try {
            String json = objectMapper.writeValueAsString(UserVO.from(dbUser));
            stringRedisTemplate.opsForValue().set(
                    key,
                    json,
                    30,
                    TimeUnit.MINUTES
            );
        } catch (JacksonException e) {
            log.error("用户数据序列化失败，写入Redis缓存异常, userId={}", userId, e);
        }
        return UserVO.from(dbUser);
    }

    // ========== 4. 刷新令牌 ==========
    @Override
    public LoginVO refresh(String refreshToken) {
        // 1. 用正向索引查是哪个用户
        String userIdStr = stringRedisTemplate.opsForValue().get("refresh_token:" + refreshToken);
        if (userIdStr == null) {
            throw new BusinessException("刷新令牌已过期或不存在");
        }
        Integer userId = Integer.valueOf(userIdStr);

        // 2. 删除旧的 Refresh Token，正向和反向都要删（令牌轮换）
        stringRedisTemplate.delete("refresh_token:" + refreshToken);
        stringRedisTemplate.delete("user_refresh:" + userId);

        // 3. 查用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 4. 生成新的 Access Token
        String newAccessToken = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        // 5. 生成新的 Refresh Token，正向和反向都要写
        String newRefreshToken = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(
                "refresh_token:" + newRefreshToken,
                String.valueOf(user.getId()),
                7, TimeUnit.DAYS
        );
        stringRedisTemplate.opsForValue().set(
                "user_refresh:" + userId,
                newRefreshToken,
                7, TimeUnit.DAYS
        );

        // 6. 组装返回
        LoginVO vo = new LoginVO();
        vo.setToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setUserInfo(UserVO.from(user));

        return vo;
    }

    // ========== 5. 登出   ==========
    @Override
    public void logout(Integer userId, String accessToken) {
        log.info("用户登出: userId={}", userId);

        //删除刷新令牌正反向索引
        String refreshToken = stringRedisTemplate.opsForValue()
                .get("user_refresh:" + userId);

        if (refreshToken != null) {
            stringRedisTemplate.delete("refresh_token:" + refreshToken);
            stringRedisTemplate.delete("user_refresh:" + userId);
        }

        //  access token 加入黑名单
        if (accessToken != null && !accessToken.isBlank()) {
            String key = "blacklist:user:" + userId;
            long expireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(120);

            stringRedisTemplate.opsForHash()
                    .put(key, accessToken, String.valueOf(expireTime));

            // 建议给整个 key 设置过期时间，避免黑名单 key 永不过期
            stringRedisTemplate.expire(key, 120, TimeUnit.MINUTES);
        }
    }

    @Override
    public AuthorProfileVO getAuthorProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->  new BusinessException("书评人不存在"));

        Long count = contentRepository.count(
                Specification.allOf(
                        ContentSpecifications.notDeleted(),
                        ContentSpecifications.authorIdEquals(userId)
                )
        );
        AuthorProfileVO vo = new AuthorProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setBio(user.getBio());
        vo.setAreas(user.getAreas());
        vo.setIsAi(user.getIsAi() != null && user.getIsAi() == 1);
        vo.setContentCount(count != null ? count : 0L);

        return vo;
    }
}
