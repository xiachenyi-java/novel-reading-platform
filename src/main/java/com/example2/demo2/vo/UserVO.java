package com.example2.demo2.vo;

import com.example2.demo2.entity.User;
import lombok.Data;

/**
 * 夏辰义
 * 2026/10/1915:38
 */
@Data
public class UserVO {

    private Integer id;

    private String username;

    private String role;


    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        return vo;
    }

}
