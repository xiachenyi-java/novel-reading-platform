package com.example2.demo2.task;

import com.example2.demo2.repository.BookshelfRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 夏辰义
 * 2026/10/1013:34
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgressFlushTask {
    private final StringRedisTemplate stringRedisTemplate;
    private final BookshelfRepository bookshelfRepository;

    private record ProgressData(Integer userId,
                                Integer novelId,
                                Integer chapterId,
                                LocalDateTime readTime) {
    }

    // 上一次结束后再等 5 分钟。验证时可临时改成 10000。
    @Scheduled(fixedDelay = 300000)
    @Transactional(rollbackFor = Exception.class)
    public void flushProgress() {
        int scanned = 0;
        int updated = 0;

        List<ProgressData> dataList = new ArrayList<>();

        // SCAN，不用 KEYS。Cursor 必须关闭，它占着 Redis 连接。
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions()
                        .match("progress:*")
                        .count(100)
                        .build())) {

            while (cursor.hasNext()) {
                String key = cursor.next();
                scanned++;

                try {
                    String[] parts = key.split(":");
                    if (parts.length != 3) {
                        log.warn("进度 key 格式不对，跳过：{}", key);
                        continue;
                    }

                    Integer userId = Integer.valueOf(parts[1]);
                    Integer novelId = Integer.valueOf(parts[2]);

                    Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
                    String chapterIdStr = (String) entries.get("chapterId");
                    String updateTimeStr = (String) entries.get("updateTime");

                    if (chapterIdStr == null || updateTimeStr == null) {
                        log.warn("进度字段缺失，跳过：{}", key);
                        continue;
                    }

                    Integer chapterId = Integer.valueOf(chapterIdStr);
                    long ts = Long.parseLong(updateTimeStr);
                    LocalDateTime readTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(ts),
                            ZoneId.systemDefault()
                    );

                    dataList.add(new ProgressData(userId, novelId, chapterId, readTime));
                } catch (Exception e) {
                    log.warn("解析单条 Redis 进度失败，跳过。key={}", key, e);
                }
            }
        } catch (Exception e) {
            log.warn("遍历 Redis 进度失败，本轮跳过", e);
            return;
        }

        // 关闭 Cursor 后再做数据库更新，避免数据库异常被外层 catch 吞掉
        LocalDateTime now = LocalDateTime.now();
        for (ProgressData data : dataList) {
            int rows = bookshelfRepository.updateProgressFromRedis(
                    data.userId(),
                    data.novelId(),
                    data.chapterId(),
                    data.readTime(),
                    now
            );
            if (rows > 0) {
                updated++;
            }
        }

        log.info("进度刷回完成：扫描 {} 条，更新 {} 条", scanned, updated);
    }
}
