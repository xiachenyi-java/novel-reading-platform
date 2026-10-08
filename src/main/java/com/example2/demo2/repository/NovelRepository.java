package com.example2.demo2.repository;

import com.example2.demo2.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Integer>, JpaSpecificationExecutor<Novel> {

    //数据库中获取分类
    @Query("SELECT DISTINCT n.category FROM Novel n ORDER BY n.category")
    List<String> findDistinctCategories();
}
