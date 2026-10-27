package com.example2.demo2.controller;

import com.example2.demo2.common.Result;
import com.example2.demo2.common.annotation.RequireAdmin;
import com.example2.demo2.service.Iservice.IContentService;
import com.example2.demo2.vo.ContentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 夏辰义
 * 2026/10/2714:05
 */
@Tag(name = "内容流")
@RestController
@RequiredArgsConstructor
@RequestMapping("/contents")
public class ContentController {
    private final IContentService contentService;

    @Operation(summary = "获取内容流列表")
    @GetMapping
    public Result<Page<ContentVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String polarity,
            @RequestParam(required = false) Integer novelId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String keyword
    ){
        //安全兜底
        if (page < 1) {page = 1;}
        if (size < 1) {size = 10;}
        if (size > 100) {size = 100;}

        Page<ContentVO> result = contentService.findPageContents(
                page, size, polarity, novelId, userId, keyword);

        return Result.success(result);
    }

    @RequireAdmin
    @Operation(summary = "删除书评")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        contentService.softDelete(id);
        return Result.success();
    }
}
