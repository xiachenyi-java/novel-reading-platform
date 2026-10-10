package com.example2.demo2.service;

import com.example2.demo2.entity.Bookshelf;
import com.example2.demo2.enums.BookshelfSort;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 夏辰义
 * 2026/10/816:16
 */
public interface IBookshelfService {

    //加入书架
    Bookshelf addBookshelf(Integer userId, Integer novelId);

    //书架列表
    Page<Bookshelf> findPageBookshelf(Integer userId,int page, int size, BookshelfSort sort);

    //置顶和取消置顶
    void top(Integer userId, Integer BookshelfId, Boolean isTop);

    //软删除书架里的书
    void remove(Integer userId,Integer BookshelfId );

    //删除多本
    void batchRemove(Integer userId, List<Integer> ids);

    void updateProgress(Integer userId,Integer novelId,Integer chapterId,Integer chapterNumber);
}
