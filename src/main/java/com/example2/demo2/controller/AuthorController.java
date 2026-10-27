package com.example2.demo2.controller;

import com.example2.demo2.common.Result;
import com.example2.demo2.service.Iservice.IUserService;
import com.example2.demo2.vo.AuthorProfileVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 夏辰义
 * 2026/10/2715:18
 */
@Tag(name = "书评人主页")
@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final IUserService userService;

    @GetMapping("/{id}")
    public Result<AuthorProfileVO> getProfile(@PathVariable Integer id) {
        return Result.success(userService.getAuthorProfile(id));
    }
}
