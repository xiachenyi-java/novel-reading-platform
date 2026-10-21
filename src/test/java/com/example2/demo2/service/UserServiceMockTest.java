package com.example2.demo2.service;

import com.example2.demo2.common.JwtUtil;
import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.dto.LoginDTO;
import com.example2.demo2.entity.User;
import com.example2.demo2.repository.UserRepository;
import com.example2.demo2.vo.LoginVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 夏辰义
 * 2026/10/1915:21
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceMockTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;  // ← 链式 Mock 的关键

    @InjectMocks
    private UserServiceIml userService;

    // ========== 1. 分支：用户不存在 → 抛异常，不碰 Redis / JWT ==========
    @Test
    void testLoginUserNotFound() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ghost");
        dto.setPassword("whatever");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(dto));

        assertEquals("用户名或密码错误", ex.getMessage());

        // 关键断言：走到 orElseThrow 就断了，后面的依赖一个都不该被调
        verify(jwtUtil, never()).generateToken(any(), any(), any());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    // ========== 2. 分支：密码错误 → 抛异常，不碰 Redis / JWT ==========
    @Test
    void testLoginWrongPassword() {
        User fakeUser = new User();
        fakeUser.setId(1);
        fakeUser.setUsername("testuser");
        fakeUser.setPasswordHash("$2a$fakehash");

        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrong");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(fakeUser));
        when(bCryptPasswordEncoder.matches("wrong", "$2a$fakehash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(dto));

        assertEquals("用户名或密码错误", ex.getMessage());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    // ========== 3. 主流程：成功登录 → 返回 token + userInfo ==========
    @Test
    void testLoginSuccess() {
        User fakeUser = new User();
        fakeUser.setId(1);
        fakeUser.setUsername("testuser");
        fakeUser.setRole("USER");
        fakeUser.setPasswordHash("$2a$fakehash");

        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(fakeUser));
        when(bCryptPasswordEncoder.matches("123456", "$2a$fakehash")).thenReturn(true);
        when(jwtUtil.generateToken(1, "testuser", "USER")).thenReturn("fake-jwt-token");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);  // ← 链式 Mock 入口

        LoginVO vo = userService.login(dto);

        // 契约断言：调用者能看到什么
        assertNotNull(vo.getToken());
        assertEquals("fake-jwt-token", vo.getToken());
        assertNotNull(vo.getRefreshToken());          // UUID 是真随机，只断言不为空
        assertNotNull(vo.getUserInfo());
        assertEquals("testuser", vo.getUserInfo().getUsername());

        // ★ 任务 27 的主断言：密码绝不能出现在响应体里
        //assertNull(vo.getUserInfo().getPasswordHash());

        // 副作用断言：当前实现是「原地清空 fakeUser」
        // 任务 27 加 UserVO 后这一条已失效（不再修改传入实体），故注释掉
        //assertNull(fakeUser.getPasswordHash());

        // verify：两条 Redis 索引都写了，过期 7 天
        verify(stringRedisTemplate.opsForValue(), times(2))
                .set(anyString(), anyString(), eq(7L), eq(TimeUnit.DAYS));
    }
}
