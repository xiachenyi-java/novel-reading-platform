package com.example2.demo2.repository;

import com.example2.demo2.entity.Novel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 夏辰义
 * 2026/9/2721:53
 */
public class NovelSpecifications {

    private NovelSpecifications() {
    }//私有构造

    //按书名模糊查
    public static Specification<Novel> titleContains(String keyword) {
        return (root, query, cb) ->
                StringUtils.hasText(keyword)
                        ? cb.like(root.get("title"), "%" + keyword + "%")
                        : null;
    }

    //按分类分
    public static Specification<Novel> categoryEquals(String category) {
        return ((root, query, cb) ->
                StringUtils.hasText(category)
                        ? cb.equal(root.get("category"), category)
                        : null);
    }

    //按状态分
    public static Specification<Novel> statusEquals(String status) {
        return ((root, query, cb) ->
                StringUtils.hasText(status)
                        ? cb.equal(root.get("status"), status)
                        : null);
    }
}
