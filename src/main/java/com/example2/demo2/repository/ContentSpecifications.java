package com.example2.demo2.repository;

import com.example2.demo2.entity.Content;
import com.example2.demo2.enums.Polarity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
/**
 * 夏辰义
 * 2026/10/2416:09
 */
public class ContentSpecifications {

    private ContentSpecifications(){}

    // 软删过滤，每个查询都必须带 —— 单独抽出来，防止漏
    public static Specification<Content> notDeleted() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), 0);
    }

    public static Specification<Content> polarityEquals(Polarity polarity) {
        return (root, query, cb) ->
                polarity == null ? null : cb.equal(root.get("polarity"), polarity);
    }

    public static Specification<Content> novelIdEquals(Integer novelId) {
        return (root, query, cb) ->
                novelId == null ? null : cb.equal(root.get("novelId"), novelId);
    }

    public static Specification<Content> authorIdEquals(Integer authorId) {
        return (root, query, cb) ->
                authorId == null ? null : cb.equal(root.get("authorId"), authorId);
    }

    public static Specification<Content> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String like = "%" + keyword + "%";
            return cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("body"), like)
            );
        };
    }
}
