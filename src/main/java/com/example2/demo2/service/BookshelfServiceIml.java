package com.example2.demo2.service;

import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.entity.Bookshelf;
import com.example2.demo2.entity.Novel;
import com.example2.demo2.repository.BookshelfRepository;
import com.example2.demo2.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        Bookshelf existing  = bookshelfRepository.findByUserIdAndNovelId(userId,novelId).orElse(null);
        if (existing != null ) {
            if (existing.getDeleted() == 0){
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
}
