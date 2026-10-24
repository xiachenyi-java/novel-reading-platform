package com.example2.demo2.repository;


import com.example2.demo2.entity.ContentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 夏辰义
 * 2026/10/2416:21
 */
@Repository
public interface ContentMarkRepository extends JpaRepository<ContentMark,Integer> {

    // 单条书评的所有标签
    List<ContentMark> findByContentId(Integer contentId);

    // ★ 批量查：列表接口一次性捞回这一页所有书评的标签，避免 N+1
    List<ContentMark> findByContentIdIn(Collection<Integer> contentIds);
}
