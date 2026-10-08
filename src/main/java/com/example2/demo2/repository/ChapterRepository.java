package com.example2.demo2.repository;

import com.example2.demo2.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    List<Chapter> findByNovelIdOrderByChapterNumberAsc(Integer novelId);

    void deleteByNovelId(Integer novelId);
}
