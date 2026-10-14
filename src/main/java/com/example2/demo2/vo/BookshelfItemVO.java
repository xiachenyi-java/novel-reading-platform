package com.example2.demo2.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 夏辰义
 * 2026/10/1414:47
 */
@Data
public class BookshelfItemVO{
    // ========== 来自 Bookshelf ==========
    private Integer id;                    // 书架记录 id（前端置顶/移出用）
    private Boolean top;                   // 是否置顶
    private LocalDateTime topTime;
    private Integer lastReadChapterId;    // 最后阅读时间
    private LocalDateTime lastReadTime;
    private LocalDateTime createTime;      // 加入书架时间

    // ========== 来自 Novel ==========
    private Integer novelId;               // 小说 id（点进详情用）
    private String title;
    private String author;
    private String coverUrl;
    private String category;
    private String status;
    private Integer totalWords;
    private LocalDateTime lastUpdateTime;  // 小说最近更新时间
}
