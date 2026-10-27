package com.example2.demo2.vo;

import com.example2.demo2.enums.Grade;
import com.example2.demo2.enums.Polarity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 夏辰义
 * 2026/10/2417:00
 */
@Data
public class ContentVO {
    //     书评本身
    private Integer id;
    private Polarity polarity;
    private Grade grade;
    private String title;
    private String text;
    private LocalDateTime createTime;

    //     作者信息
    private AuthorBrief author;

    //     书籍信息
    private NovelBrief novel;

    //内部类，作者信息
    @Data
    public static class AuthorBrief {
        private Integer id;
        private String username;
        private Boolean isAi;
    }

    @Data
    public static class NovelBrief {
        private Integer id;
        private String title;
        private String author;
        private String category;
        private String coverUrl;
    }
}
