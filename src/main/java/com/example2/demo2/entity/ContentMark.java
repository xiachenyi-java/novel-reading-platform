package com.example2.demo2.entity;

import com.example2.demo2.enums.MarkKind;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 夏辰义
 * 2026/10/2415:40
 */
@Entity
@Data
@Table(
        name = "content_mark",
        indexes = {
                @Index(name = "idx_mark_content",columnList = "contentId"),
                @Index(name = "idx_mark_tag",columnList = "kind,tag")
        }
)
public class ContentMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer contentId;//关联书评

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 10)
    private MarkKind kind;//推荐点，避雷点

    @Column(nullable = false,length = 50)
    private String tag;//小说现状
}
