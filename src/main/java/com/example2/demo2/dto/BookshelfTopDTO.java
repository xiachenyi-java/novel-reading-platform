package com.example2.demo2.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 夏辰义
 * 2026/10/823:25
 */
@Data
public class BookshelfTopDTO {
    @NotNull(message = "置顶状态不能为空")
    private Integer top;   // 0 未置顶或 1置顶
}
