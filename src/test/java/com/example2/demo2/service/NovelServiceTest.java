package com.example2.demo2.service;

import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.dto.NovelDTO;
import com.example2.demo2.entity.Novel;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 夏辰义
 * 2026/8/1519:14
 */
@SpringBootTest
//启动整个 Spring Boot 应用，加载所有 Bean
@Transactional
//每个测试方法跑完后，数据库操作全部回滚"
public class NovelServiceTest  {

    @Autowired
    private INovelService INovelService;

    @Test
    void  testAdd(){
        //准备
        NovelDTO dto = new NovelDTO();
        dto.setTitle("单元测试小说");
        dto.setSummary("测试简介");
        dto.setCategory("玄幻");
        //执行
        Novel novel = INovelService.addNovel(dto);
        //断言
        assertNotNull(novel.getId());
        assertEquals("单元测试小说", novel.getTitle());
    }

    @Test
    void testFindByIdNotFound(){
        Exception exception = assertThrows(RuntimeException.class,() -> {
            INovelService.findDetail(99999);});
        assertTrue(exception.getMessage().contains("小说不存在"));
    }

    @Test
    void testDelete(){
        // 准备：先加一个
        NovelDTO dto = new NovelDTO();
        dto.setTitle("待删除的小说");
        dto.setSummary("简介");
        dto.setCategory("玄幻");
        Novel novel = INovelService.addNovel(dto);
        Integer id = novel.getId();

        // 执行
        INovelService.deleteNovel(id);

        // 断言：删完再查，应该抛
        assertThrows(BusinessException.class, () -> INovelService.findDetail(id));
    }
}
