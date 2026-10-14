package com.example2.demo2.service;

import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.entity.Bookshelf;
import com.example2.demo2.entity.Novel;
import com.example2.demo2.enums.BookshelfSort;
import com.example2.demo2.repository.BookshelfRepository;
import com.example2.demo2.repository.NovelRepository;
import com.example2.demo2.service.Iservice.IBookshelfService;
import com.example2.demo2.vo.BookshelfItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 夏辰义
 * 2026/10/816:16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookshelfServiceIml implements IBookshelfService {

    private final NovelRepository novelRepository;

    private final BookshelfRepository bookshelfRepository;

    private final StringRedisTemplate  stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Bookshelf addBookshelf(Integer userId, Integer novelId) {

        //判断小说是否存在
        novelRepository.findById(novelId).orElseThrow(() -> new BusinessException("小说不存在"));

        //判断是否在书架中
        Bookshelf existing = bookshelfRepository.findByUserIdAndNovelId(userId, novelId).orElse(null);
        if (existing != null) {
            if (existing.getDeleted() == 0) {
                log.info("用户已在书架中，幂等返回。userId={}, novelId={}", userId, novelId);
                return existing;
            }
            existing.setDeleted(0);
            existing.setCreateTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            return bookshelfRepository.save(existing);
        }

        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setUserId(userId);
        bookshelf.setNovelId(novelId);
        bookshelf.setIsTop(0);//不置顶
        bookshelf.setDeleted(0);//存在书架
        return bookshelfRepository.save(bookshelf);
    }

    //书架列表
    @Override
    public Page<BookshelfItemVO> findPageBookshelf(Integer userId, int page, int size, BookshelfSort sort) {
        int pageIndex = page - 1;

        // ========== 1. 分页查 Bookshelf（保持你原来的逻辑，一个字不改）==========
        Page<Bookshelf> bookshelfPage;
        if (sort == BookshelfSort.NOVEL_UPDATE) {
            Pageable pageable = PageRequest.of(pageIndex, size);
            bookshelfPage = bookshelfRepository.findPageOrderByNovelUpdate(userId, pageable);
        } else {
            Sort s = buildSort(sort);
            Pageable pageable = PageRequest.of(pageIndex, size, s);
            bookshelfPage = bookshelfRepository.findByUserIdAndDeleted(userId, 0, pageable);
        }

        // ========== 2. 映射成 VO ==========
        return toItemVOPage(bookshelfPage);
    }

    // ========== 分页对象批量映射 ==========
    private Page<BookshelfItemVO> toItemVOPage(Page<Bookshelf> page) {
        // 2.1 空页直接返回，避免多余查询
        if (page.getContent().isEmpty()) {
            return page.map(b -> null);   // 内容为空，返回空 VO 页
        }

        // 2.2 一次查出本页涉及的所有小说（去重后批量查，避免 N+1）
        List<Integer> novelIds = page.getContent().stream()
                .map(Bookshelf::getNovelId)
                .distinct()
                .toList();

        Map<Integer, Novel> novelMap = novelRepository.findAllById(novelIds).stream()
                .collect(Collectors.toMap(Novel::getId, Function.identity()));

        // 2.3 用 Page.map 保留分页元信息（totalElements / totalPages 都不丢）
        return page.map(b -> toItemVO(b, novelMap.get(b.getNovelId())));
    }

    // ========== 单条映射 ==========
    private BookshelfItemVO toItemVO(Bookshelf b, Novel n) {
        BookshelfItemVO vo = new BookshelfItemVO();

        // 书架字段
        vo.setId(b.getId());
        vo.setTop(b.getIsTop() != null && b.getIsTop() == 1);   // 0/1 → boolean
        vo.setTopTime(b.getTopTime());
        vo.setLastReadChapterId(b.getLastReadChapterId());
        vo.setLastReadTime(b.getLastReadTime());
        vo.setCreateTime(b.getCreateTime());

        // 小说字段（小说可能已被删除，用 null 兜底）
        vo.setNovelId(b.getNovelId());     // ← 这个永远有，来自书架记录
        if (n != null) {
            vo.setTitle(n.getTitle());
            vo.setAuthor(n.getAuthor());
            vo.setCoverUrl(n.getCoverUrl());
            vo.setCategory(n.getCategory());
            vo.setStatus(n.getStatus());
            vo.setTotalWords(n.getTotalWords());
            vo.setLastUpdateTime(n.getLastUpdateTime());
        }

        return vo;
    }

    //置顶
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void top(Integer userId, Integer BookshelfId, Boolean isTop) {
        Bookshelf bookshelf = bookshelfRepository.findByIdAndUserIdAndDeleted(BookshelfId, userId,1)
                .orElseThrow(() -> new BusinessException("小说不在书架"));
        bookshelf.setIsTop(isTop ? 1 : 0);
        if (bookshelf.getIsTop() == 0) {
            bookshelf.setTopTime(null);
        }

        if (bookshelf.getIsTop() == 1) {
            bookshelf.setTopTime(LocalDateTime.now());
        }
        bookshelfRepository.save(bookshelf);
    }

    //软删除
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void remove(Integer userId, Integer BookshelfId) {
        Bookshelf bookshelf = bookshelfRepository.findByIdAndUserIdAndDeleted(BookshelfId, userId,1)
                .orElseThrow(() -> new BusinessException("书不存在"));
        //先删数据库
        bookshelf.setDeleted(1);
        bookshelf.setIsTop(0);
        bookshelf.setTopTime(null);
        bookshelf.setLastReadChapterId(null);
        bookshelf.setLastReadTime(null);

        //再删缓存
        try {
            String progressKey = "progress:" + userId + ":" + bookshelf.getNovelId();
            stringRedisTemplate.delete(progressKey);
        } catch (Exception e) {
            // Redis 抖动不该影响「移出书架」这个主流程
            log.warn("清进度缓存失败，忽略。userId={}, novelId={}",
                    userId, bookshelf.getNovelId(), e);
        }

        bookshelfRepository.save(bookshelf);

    }

    //批量删除
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemove(Integer userId, List<Integer> ids) {
        int affected = bookshelfRepository.softDeleteByIds(
                ids,
                userId,
                LocalDateTime.now());

        if(affected < ids.size()){
            log.info("批量移出部分成功：请求 {} 条，实际移出 {} 条（其余为他人记录/已移出/不存在）",
                    ids.size(), affected);
        }
    }

    @Override
    public void updateProgress(Integer userId, Integer novelId, Integer chapterId, Integer chapterNumber) {
        try {
            String key = "progress:" + userId + ":" + novelId;

            Map<String, String> progress = new HashMap<>();
            progress.put("chapterId", String.valueOf(chapterId));
            progress.put("chapterNumber", String.valueOf(chapterNumber));
            progress.put("updateTime", String.valueOf(System.currentTimeMillis()));

            stringRedisTemplate.opsForHash().putAll(key, progress);
            stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            // 进度写入失败不能影响正常阅读，降级忽略
            log.warn("阅读进度写入 Redis 失败，降级忽略。userId={}, novelId={}, chapterId={}",
                    userId, novelId, chapterId, e);
        }
    }

    private Sort buildSort(BookshelfSort sort) {
        return switch (sort) {
            case RECENT_ADD -> Sort.by(
                    Sort.Order.desc("isTop"),
                    Sort.Order.desc("topTime"),
                    Sort.Order.desc("createTime"),
                    Sort.Order.desc("id"));
            default -> Sort.by(                        // RECENT_READ
                    Sort.Order.desc("isTop"),
                    Sort.Order.desc("topTime"),
                    Sort.Order.desc("lastReadTime"),
                    Sort.Order.desc("id"));
        };
    }
}
