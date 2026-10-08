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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public Bookshelf top(Integer userId, Integer id, Integer isTop) {
        Bookshelf bookshelf = bookshelfRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("小说不在书架"));
        bookshelf.setIsTop(isTop);
        if (bookshelf.getIsTop() == 0) {
            bookshelf.setTopTime(null);
        }

        if (bookshelf.getIsTop() == 1) {
            bookshelf.setTopTime(LocalDateTime.now());
        }
        return bookshelfRepository.save(bookshelf);
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
