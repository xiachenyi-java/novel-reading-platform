package com.example2.demo2.service.Iservice;

import com.example2.demo2.dto.LoginDTO;
import com.example2.demo2.dto.UserRegisterDTO;
import com.example2.demo2.entity.User;
import com.example2.demo2.vo.LoginVO;

public interface IUserService {

    // 1.用户登陆
    LoginVO login(LoginDTO dto);

    // 2.用户注册
    User register(UserRegisterDTO dto);

    // 3.获取当前用户
    User getCurrentUser();

    // 4.刷新令牌
    LoginVO refresh(String refreshToken);

    //5.用户登出
    void logout(Integer userId, String accessToken);
}
