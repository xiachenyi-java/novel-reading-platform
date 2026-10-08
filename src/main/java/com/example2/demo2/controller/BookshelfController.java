package com.example2.demo2.controller;

import com.example2.demo2.common.Result;
import com.example2.demo2.common.UserContext;
import com.example2.demo2.dto.BookshelfAddDTO;
import com.example2.demo2.entity.Bookshelf;
import com.example2.demo2.service.IBookshelfService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 夏辰义
 * 2026/10/816:48
 */
@RestController
@RequestMapping("/bookshelfs")
@RequiredArgsConstructor
public class BookshelfController {

    private final IBookshelfService iBookshelfService;

    @Operation(summary = "加入书架")
    @PostMapping
    public Result<Bookshelf> addBookshelf(@RequestBody  @Valid BookshelfAddDTO dto){

        //从当前线程拿到用户id
        Integer userId = UserContext.getUser().getUserId();

        return Result.success(iBookshelfService.addBookshelf(userId,dto.getNovelId()));
    }
}
