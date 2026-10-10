package com.example2.demo2.service;

import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.entity.Bookshelf;
import com.example2.demo2.enums.BookshelfSort;
import com.example2.demo2.repository.BookshelfRepository;
import com.example2.demo2.repository.NovelRepository;
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
    public Page<Bookshelf> findPageBookshelf(Integer userId, int page, int size, BookshelfSort sort) {
        int pageIndex = page - 1;//分页换算

        //按更新时间排序
        if (sort == BookshelfSort.NOVEL_UPDATE) {
            Pageable pageable = PageRequest.of(pageIndex, size);
            return bookshelfRepository.findPageOrderByNovelUpdate(userId, pageable);
        }

        Sort s = buildSort(sort);
        Pageable pageable = PageRequest.of(pageIndex, size, s);
        return bookshelfRepository.findByUserIdAndDeleted(userId, 0, pageable);

    }

    //置顶
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void top(Integer userId, Integer BookshelfId, Boolean isTop) {
        Bookshelf bookshelf = bookshelfRepository.findByIdAndUserId(BookshelfId, userId)
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
        Bookshelf bookshelf = bookshelfRepository.findByIdAndUserId(BookshelfId, userId).orElseThrow(() -> new BusinessException("书不存在"));
        bookshelf.setDeleted(1);
        bookshelf.setIsTop(0);
        bookshelf.setTopTime(null);
        bookshelf.setLastReadChapterId(null);
        bookshelf.setLastReadTime(null);
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
