package com.example2.demo2.controller;

import com.example2.demo2.common.Result;
import com.example2.demo2.common.UserContext;
import com.example2.demo2.dto.BookshelfAddDTO;
import com.example2.demo2.dto.BookshelfTopDTO;
import com.example2.demo2.entity.Bookshelf;
import com.example2.demo2.enums.BookshelfSort;
import com.example2.demo2.service.IBookshelfService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;



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

    @Operation(summary = "书架列表")
    @GetMapping
    public Result<Page<Bookshelf>> listBookshelf(@RequestParam(defaultValue = "1")   Integer page,
                                                 @RequestParam(defaultValue = "10")   Integer size,
                                                 @RequestParam(defaultValue = "RECENT_READ")  BookshelfSort sort){
        if (page < 1)  {page = 1;}
        if (size < 1)  {size = 10;}
        if (size > 100) {size = 100;}

        Integer userId = UserContext.getUser().getUserId();

        return Result.success(iBookshelfService.findPageBookshelf(userId,page,size,sort));
    }

    @Operation(summary = "置顶/取消置顶")
    @PutMapping
    public Result<Bookshelf> top(@PathVariable  Integer id,
                                 @RequestParam @Valid BookshelfTopDTO dto){

        Integer userId = UserContext.getUser().getUserId();

        return Result.success(iBookshelfService.top(userId,id,dto.getTop()));
    }

}
