package com.example2.demo2.config;

import com.example2.demo2.enums.BookshelfSort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 夏辰义
 * 2026/10/822:45
 */
//转化URL字符串变成BookshelfSort 枚举
@Component
public class BookshelfSortConverter implements Converter<String, BookshelfSort> {

    @Override
    public BookshelfSort convert(String source){
        if (source == null || source.isBlank()){
            return BookshelfSort.RECENT_READ;
        }

        String normalized  = source.trim().toUpperCase(Locale.ROOT);
        try {
            return BookshelfSort.valueOf(normalized);
        }catch (IllegalArgumentException ex){
            return BookshelfSort.RECENT_READ;
        }
    }
}
