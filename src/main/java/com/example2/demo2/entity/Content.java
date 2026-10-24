package com.example2.demo2.entity;


import com.example2.demo2.enums.Grade;
import com.example2.demo2.enums.Polarity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 夏辰义
 * 2026/10/2415:22
 */
@Entity
@Table(
        name = "content",
        indexes = {
                @Index(name = "idx_content_feed",   columnList = "deleted,createTime"),
                @Index(name = "idx_content_novel",  columnList = "novelId,deleted,createTime"),
                @Index(name = "idx_content_author", columnList = "authorId,deleted,createTime"),
                @Index(name = "idx_content_polar",  columnList = "polarity,deleted,createTime")
        }
)
@Data
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer authorId;//书评人

    @Column(nullable = false)
    private Integer novelId;//关联小说

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Polarity polarity;//推荐分类

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Grade grade;//书籍评级

    @Column(nullable = false)
    private String title;//标题

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;//内容

    @Column(nullable = false)
    private LocalDateTime createTime;//发布时间

    @Column(nullable = false)
    private Integer deleted = 0;// 软删除，0=正常，1=已删

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
