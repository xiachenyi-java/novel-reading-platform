package com.example2.demo2.controller;

import java.util.Set;

import com.example2.demo2.entity.User;
import com.example2.demo2.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 夏辰义
 * 2026/8/2312:53
 */
@SpringBootTest
@AutoConfigureMockMvc   // 自动配置 MockMvc
@Transactional //事务
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;//模拟 HTTP 客户端

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ========== 重置限流计数 ==========
    // 清空 rate_limit:* —— 登录接口的限流计数存在 Redis 里，
    // @Transactional 回滚不了它，会导致测试不可重复。
    // 详细踩坑过程见 docs/项目笔记/11-测试体系.txt 第五节 · 坑 1
    @BeforeEach
    void resetRateLimit() {
        Set<String> keys = stringRedisTemplate.keys("rate_limit:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @BeforeEach
    void setUp(){
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole("USER");
        userRepository.save(user);
    }

    @Test
    void testRegisterSuccess() throws Exception{
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"newuser\",\"password\":\"123456\"}"))
                        .andExpect(status().isOk())//HTTP 200
                .andExpect(jsonPath("$.code").value(200));
        // 你的 Result.code 是 200
    }

    @Test
    void testRegisterDuplicate() throws Exception {
        // 再注册一次 setUp 里已经有的 "testuser"
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))   // 你的 Result 错误码是 500
                .andExpect(jsonPath("$.msg").value("用户名已存在"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())  // 返回里有 token
                .andExpect(jsonPath("$.data.userInfo.username").value("testuser"));
    }

    // 密码错误时登录失败
    @Test
    void testLoginWithWrongPassword_ShouldReturn500() throws Exception {
        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    // 不带 token 创建小说，应被拦截返回 401
    @Test
    void testCreateNovelWithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/novels").
                        contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"title\":\"测试小说\",\"category\":\"玄幻\",\"status\":\"连载中\"}")
                )).andExpect(status().isUnauthorized());
    }

    // 带有效 token 访问小说列表，返回 200
    @Test
    void testAccessNovelsWithValidToken_ShouldReturn200() throws Exception {
        // 1. 先登录拿到 token
        MvcResult result = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"123456\"}"))
                .andReturn();

        // 2. 从 JSON 里提取 token（用 JsonPath）
        String response = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.token");

        // 3. 带 token 访问小说接口
        mockMvc.perform(get("/novels")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUserCannotDeleteNovel() throws Exception {
        // 1. 普通用户登录拿 token
        MvcResult result = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"123456\"}"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.token");

        // 2. 普通用户调 DELETE（需要 ADMIN 权限）
        mockMvc.perform(delete("/novels/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());  // ← 403，权限不足
    }

    // ========== 黑名单生效测试 ==========
    // 登录 → 登出（token 进黑名单）→ 再用同一个 token → 401
    // 用 /users/logout 而非 /novels 测试：/novels 属于游客路径，
    // 黑名单 token 只会被当作游客放行，不会返回 401。
    @Test
    void testLogoutThenReuseToken_ShouldReturn401() throws Exception {
        // 1. 登录拿 token
        MvcResult result = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"123456\"}"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.token");

        // 2. 第一次登出：token 合法 → 应该成功，并被写进黑名单
        mockMvc.perform(post("/users/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. 再用同一个 token 登出 → 已被拉黑 → 401
        mockMvc.perform(post("/users/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
