package com.example2.demo2.config;

import com.example2.demo2.common.JwtUtil;
import com.example2.demo2.common.UserContext;
import com.example2.demo2.common.annotation.RequireAdmin;
import com.example2.demo2.dto.UserContextDTO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 夏辰义
 * 2026/8/21 17:59
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    // ========== 请求预处理：鉴权 + 权限校验 ==========
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {

        UserContext.remove();  // 先清理，保证干净

        // 预检请求（OPTIONS）直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // getServletPath() 不含上下文路径，直接用相对路径判断
        String path = request.getServletPath();
        String method = request.getMethod();

        // 游客规则：GET 请求 + 路径以 /novels 开头
        boolean isGuestEndpoint = "GET".equalsIgnoreCase(method) && path.startsWith("/novels");
        String token = request.getHeader("Authorization");

        // 去掉 Bearer 前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // ========== 第一态：游客路径（宽松）==========
        // 有 token 就解析，解析失败或命中黑名单都当游客处理，不拦截
        if (isGuestEndpoint) {
            if (token != null && !token.isEmpty()) {
                try {
                    Claims claims = jwtUtil.parseToken(token);
                    Integer userId = Integer.valueOf(claims.getSubject());
                    String hashKey = "blacklist:user:" + userId;
                    Object expireObj = stringRedisTemplate.opsForHash().get(hashKey, token);

                    boolean shouldSetContext = true;  // 默认视为合法登录用户
                    if (expireObj != null) {
                        long expireAt = Long.parseLong(expireObj.toString());
                        if (expireAt >= System.currentTimeMillis()) {
                            shouldSetContext = false;
                        } else if (expireAt < System.currentTimeMillis()) {
                            stringRedisTemplate.opsForHash().delete(hashKey, token);  // 惰性删除
                        }
                    }
                    if (shouldSetContext) {
                        UserContextDTO dto = new UserContextDTO();
                        dto.setUsername(jwtUtil.getUsernameFromToken(token));
                        dto.setUserId(jwtUtil.getUserIdFromToken(token));
                        dto.setRole(claims.get("role", String.class));
                        UserContext.setUser(dto);
                    }
                } catch (Exception e) {
                    // Token 无效，什么都不做（不打印错误，保持日志干净）
                }
            }
        }

        // ========== 第二态：严格路径（无 token / 解析失败 / 在黑名单 → 401）==========
        if (!isGuestEndpoint) {

            // 判断 token 是否存在
            if (token == null || token.isEmpty()) {
                writeUnauthorized(response);
                return false;
            }

            // 解析 token
            Claims claims;
            try {
                claims = jwtUtil.parseToken(token);
            } catch (Exception e) {
                writeUnauthorized(response);
                return false;
            }

            // 查黑名单
            Integer userId = Integer.valueOf(claims.getSubject());
            String hashKey = "blacklist:user:" + userId;
            Object expireObj = null;
            try {
                expireObj = stringRedisTemplate.opsForHash().get(hashKey, token);
            } catch (Exception e) {
                // Redis 故障时降级：跳过黑名单检查，不影响正常请求
                log.warn("缓存读取失败，userid={}", userId, e);
            }

            if (expireObj != null) {
                long expireAt = Long.parseLong(expireObj.toString());
                if (expireAt > System.currentTimeMillis()) {
                    // 在黑名单里，拦截
                    writeUnauthorized(response);
                    return false;
                } else {
                    // 已过期，删除后放行
                    try {
                        stringRedisTemplate.opsForHash().delete(hashKey, token);
                    } catch (Exception e) {
                        log.warn("缓存删除失败，userid={}", userId, e);
                    }
                }
            }

            // 设置 ThreadLocal
            UserContextDTO dto = new UserContextDTO();
            dto.setUsername(jwtUtil.getUsernameFromToken(token));
            dto.setUserId(jwtUtil.getUserIdFromToken(token));
            dto.setRole(claims.get("role", String.class));
            UserContext.setUser(dto);

            // 判断身份：方法上有 @RequireAdmin 且角色不是 ADMIN → 403
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                if (handlerMethod.hasMethodAnnotation(RequireAdmin.class)) {
                    String role = UserContext.getUser().getRole();
                    if (!"ADMIN".equals(role)) {
                        writeForbidden(response, "权限不足，需要管理员身份");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // ========== 响应工具 ==========
    private void writeUnauthorized(HttpServletResponse response)
            throws Exception {
        response.setStatus(401);                                    // HTTP 状态码 401
        response.setContentType("application/json;charset=UTF-8");  // 告诉浏览器这是 JSON
        response.getWriter().write("{\"code\":401,\"msg\":\"未登录或token无效\"}");
        response.getWriter().flush();                               // 立即发送，不要缓冲
    }

    private void writeForbidden(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(403);                                    // HTTP 状态码 403
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"" + msg + "\"}");
        response.getWriter().flush();
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 清理 ThreadLocal，防止线程池复用导致用户串号
        UserContext.remove();
    }
}
