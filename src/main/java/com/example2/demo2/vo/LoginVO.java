package com.example2.demo2.vo;

import lombok.Data;

/**
 * 夏辰义
 * 2026/8/2116:06
 */
@Data
public class LoginVO {
    private String token;//jwt令牌

    private String refreshToken;//双令牌

    private UserVO userInfo;//用户信息
}
