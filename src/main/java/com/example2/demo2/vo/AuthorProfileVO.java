package com.example2.demo2.vo;

import lombok.Data;

/**
 * 夏辰义
 * 2026/10/2714:18
 */
@Data
public class AuthorProfileVO {
    private Integer id;
    private String username;
    private String bio;
    private String areas;
    private Boolean isAi;      // 用 Boolean，不能用 boolean，保证序列化是 isAi
    private Long contentCount; // 写了多少篇书评
}
