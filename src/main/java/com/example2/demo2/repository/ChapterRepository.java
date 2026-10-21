package com.example2.demo2.repository;

import com.example2.demo2.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    List<Chapter> findByNovelIdOrderByChapterNumberAsc(Integer novelId);

    void deleteByNovelId(Integer novelId);

    //只有数量，不啦正文
    long countByNovelId(Integer novelId);

    /**
     * 两阶段重排 —— 第一阶段：把受影响章节挪到不可能冲突的高位。
     * deletedNo 之后的章节全部 +1000000。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Chapter c SET c.chapterNumber = c.chapterNumber + 1000000 " +
            "WHERE c.novelId = :novelId AND c.chapterNumber > :deletedNo")
    int shiftChapterNumbersUp(@Param("novelId") Integer novelId,
                              @Param("deletedNo") Integer deletedNo);

    /**
     * 两阶段重排 —— 第二阶段：再挪回来，整体比原来小 1。
     * 减 1000001 = 抵消 +1000000 再前移一位。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Chapter c SET c.chapterNumber = c.chapterNumber - 1000001 " +
            "WHERE c.novelId = :novelId AND c.chapterNumber > :deletedNo")
    int shiftChapterNumbersDown(@Param("novelId") Integer novelId,
                                @Param("deletedNo") Integer deletedNo);

    /**
     * 查当前小说最大的章节号（没有章节时返回 0）。
     * 注意：这里必须用 MAX，不能用 COUNT —— 删章会留下空洞，
     * COUNT+1 会撞上已存在的章节号，导致永远发布失败。
     */
    @Query("SELECT COALESCE(MAX(c.chapterNumber), 0) FROM Chapter c WHERE c.novelId = :novelId")
    Integer findMaxChapterNumber(@Param("novelId") Integer novelId);
}
