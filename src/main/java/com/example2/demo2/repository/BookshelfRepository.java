package com.example2.demo2.repository;

import com.example2.demo2.entity.Bookshelf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookshelfRepository extends JpaRepository<Bookshelf, Integer> {
    //复活逻辑，按用户和小说查唯一记录
    Optional<Bookshelf> findByUserIdAndNovelId(Integer userId, Integer novelId);

    //归属逻辑，必须带用户，防越权
    Optional<Bookshelf> findByIdAndUserId(Integer id, Integer userId);

    //分页，最近阅读和最近增加两种排序共用
    Page<Bookshelf> findByUserIdAndDeleted(Integer userId, Integer deleted, Pageable pageable);

    //分页按新更新小说排序，因为跨表必须用原生sql
    @Query(value = "SELECT b.* FROM bookshelf b " +
            "JOIN novel n ON b.novel_id = n.id " +
            "WHERE b.user_id = :userId AND b.deleted = 0 " +
            "ORDER BY b.is_top DESC, b.top_time DESC, " +
            "         n.last_update_time DESC, b.id DESC",
            countQuery = "SELECT COUNT(*) FROM bookshelf b " +
                    "JOIN novel n ON b.novel_id = n.id " +
                    "WHERE b.user_id = :userId AND b.deleted = 0",
            nativeQuery = true)
    Page<Bookshelf> findPageOrderByNovelUpdate(@Param("userId") Integer userId,
                                               Pageable pageable);

    //批量软删，一次UPDATE，防越权
    @Modifying
    @Query("UPDATE Bookshelf b SET b.deleted = 1, b.updateTime = :now " +
            "WHERE b.id IN :ids AND b.userId = :userId AND b.deleted = 0")
    int softDeleteByIds(@Param("ids") List<Integer> ids,
                        @Param("userId") Integer userId,
                        @Param("now") LocalDateTime now);
}
