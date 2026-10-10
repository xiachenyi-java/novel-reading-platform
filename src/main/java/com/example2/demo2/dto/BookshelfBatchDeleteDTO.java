package com.example2.demo2.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 夏辰义
 * 2026/10/917:42
 */
@Data
public class BookshelfBatchDeleteDTO {

    @NotEmpty(message = "请选择要移出的书")
    @Size(max = 100,message = "一次最多移出 100 本")
    private List<Integer> ids;
}
