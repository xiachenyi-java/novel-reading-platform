package com.example2.demo2.service;

import com.example2.demo2.entity.Bookshelf;
import com.example2.demo2.enums.BookshelfSort;
import org.springframework.data.domain.Page;

/**
 * 夏辰义
 * 2026/10/816:16
 */
public interface IBookshelfService {

    //加入书架
    Bookshelf addBookshelf(Integer userId, Integer novelId);

    //书架列表
    Page<Bookshelf> findPageBookshelf(Integer userId,int page, int size, BookshelfSort sort);
}
