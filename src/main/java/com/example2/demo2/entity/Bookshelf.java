package com.example2.demo2.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 夏辰义
 * 2026/10/811:06
 */

@Data
@Entity
@Table(
        name = "bookshelf",
        indexes = {
                @Index(name = "idx_bookshelf_top_create",columnList = "userId,deleted,isTop,topTime,createTime"),
                @Index(name = "idx_bookshelf_top_read",columnList = "userId,deleted,isTop,topTime,lastReadTime")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bookshelf_user_novel",
                        columnNames = "userId,novelId"
                )
        }
)
public class Bookshelf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Integer novelId;

    private Integer lastReadChapterId;//最后阅读进度,可以为空
    private LocalDateTime lastReadTime;//最后阅读时间，可以为空

    @Column(nullable = false)
    private Integer isTop = 0;//置顶,0是未置顶

    private LocalDateTime topTime;        // 可为 null（未置顶）

    //审计
    @Column(nullable = false)
    private LocalDateTime createTime;//创建时间

    @Column(nullable = false)
    private LocalDateTime updateTime;//更新时间

    @Column(nullable = false)
    private Integer deleted = 0;//删除标记，不是删除书,0是存在

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }


}
