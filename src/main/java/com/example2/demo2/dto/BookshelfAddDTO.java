package com.example2.demo2.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 夏辰义
 * 2026/10/817:01
 */
@Data
public class BookshelfAddDTO {

    @NotNull(message = "小说ID不能为空")
    private Integer novelId;
}
