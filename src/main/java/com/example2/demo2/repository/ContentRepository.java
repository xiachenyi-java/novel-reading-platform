package com.example2.demo2.repository;

import com.example2.demo2.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 夏辰义
 * 2026/10/2416:08
 */
@Repository
public interface  ContentRepository extends JpaRepository<Content,Integer>,
        JpaSpecificationExecutor<Content> {

    Optional<Content> findByIdAndDeleted(Integer id, Integer deleted);
}
