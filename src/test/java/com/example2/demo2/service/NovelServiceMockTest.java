package com.example2.demo2.service;

import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.dto.NovelDTO;
import com.example2.demo2.entity.Novel;
import com.example2.demo2.repository.NovelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * 夏辰义
 * 2026/10/1914:44
 */
@ExtendWith(MockitoExtension.class)
public class NovelServiceMockTest {

    @Mock
    private NovelRepository novelRepository;//假仓库

    @InjectMocks
    private NovelServiceIml  novelService;//真service

    @Test
    void testUpdateNovel(){
        //造一本小说
        Novel existing = new Novel();
        existing.setId(1);
        existing.setTitle("旧书名");
        existing.setTotalWords(100);

        //告诉假仓库返回这本
        when(novelRepository.findById(1)).thenReturn(Optional.of(existing));

        // 准备要改成的数据
        NovelDTO dto = new NovelDTO();
        dto.setTitle("新书名");
        dto.setAuthor("新作者");
        dto.setSummary("新简介");
        dto.setCategory("玄幻");
        dto.setCoverUrl("http://x.jpg");

        // 执行真方法
        novelService.updateNovel(1, dto);

        // 断言：对象字段真的被改了
        assertEquals("新书名", existing.getTitle());
        assertEquals("新作者", existing.getAuthor());
        assertEquals("新简介", existing.getSummary());

        // 断言：save 被调了一次（且传的就是这本）
        verify(novelRepository, times(1)).save(existing);
    }

    // ========== 2. 分支：author 为空白 → 变成「佚名」 ==========
    @Test
    void testUpdateNovelEmptyAuthor_ShouldSetAnonymous() {
        Novel existing = new Novel();
        existing.setId(2);
        existing.setTitle("旧");
        when(novelRepository.findById(2)).thenReturn(Optional.of(existing));

        NovelDTO dto = new NovelDTO();
        dto.setTitle("新");
        dto.setAuthor("   ");   // 空白，会命中那个 ternary
        dto.setSummary("简介");
        dto.setCategory("玄幻");

        novelService.updateNovel(2, dto);

        assertEquals("佚名", existing.getAuthor());
    }

    // ========== 3. 异常分支：小说不存在 → 抛业务异常，不碰 save ==========
    @Test
    void testUpdateNovelNotFound() {
        when(novelRepository.findById(999)).thenReturn(Optional.empty());

        NovelDTO dto = new NovelDTO();
        dto.setTitle("随便");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> novelService.updateNovel(999, dto));

        assertEquals("小说不存在", ex.getMessage());
        // 关键断言：没查到就不该去 save
        verify(novelRepository, never()).save(any(Novel.class));
    }
}
