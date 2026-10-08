package com.example2.demo2.service;

import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.dto.ChapterDTO;
import com.example2.demo2.dto.NovelDTO;
import com.example2.demo2.entity.Chapter;
import com.example2.demo2.entity.Novel;
import com.example2.demo2.repository.ChapterRepository;
import com.example2.demo2.repository.NovelRepository;
import com.example2.demo2.repository.NovelSpecifications;
import com.example2.demo2.vo.NovelDetailVO;
import com.example2.demo2.vo.NovelRankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 夏辰义
 * 2026/8/26 18:02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovelServiceIml implements INovelService {

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheManager cacheManager;
    private final ConcurrentHashMap<Integer,Object> locks = new ConcurrentHashMap<>();

    // ========== 1. 创建小说 ==========
    @Override
    @CacheEvict(value = "novel-null", key = "#result.id")
    public Novel addNovel(NovelDTO dto) {
        Novel novel = new Novel();
        novel.setTitle(dto.getTitle());
        novel.setAuthor(dto.getAuthor()
        == null || dto.getAuthor().isBlank()
                ? "佚名"
                :dto.getAuthor());
        novel.setSummary(dto.getSummary());
        novel.setCategory(dto.getCategory());
        novel.setCoverUrl(dto.getCoverUrl());
        novel.setStatus("ONGOING");   // 默认连载中
        novel.setTotalWords(0);
        return novelRepository.save(novel);
    }

    // ========== 2. 发布章节（核心） ==========
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "novel", key = "#novelId")
    @Override
    public Chapter addChapter(Integer novelId, ChapterDTO dto) {
        // 2.1 查小说存在
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new BusinessException("小说不存在"));

        // 2.2 算这是第几章
        List<Chapter> list = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        int nextNumber = list.size() + 1;

        // 2.3 算字数（正文长度）
        int wordCount = dto.getContent().length();

        // 2.4 保存章节
        Chapter chapter = new Chapter();
        chapter.setNovelId(novelId);
        chapter.setChapterNumber(nextNumber);
        chapter.setTitle(dto.getTitle());
        chapter.setContent(dto.getContent());
        chapter.setWordCount(wordCount);
        chapterRepository.save(chapter);

        // 2.5 更新小说的总字数和最后更新时间
        novel.setTotalWords((novel.getTotalWords() == null ? 0 : novel.getTotalWords()) + wordCount);
        novel.setLastUpdateTime(LocalDateTime.now());
        novelRepository.save(novel);

        log.info("发布章节: novelId={}, chapterNo={}, title={}", novelId, nextNumber, dto.getTitle());
        return chapter;
    }

    // ========== 3. 修改章节 ==========
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "novel", key = "#novelId")
    @Override
    public Chapter updateChapter(Integer novelId, Integer chapterId, ChapterDTO dto) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BusinessException("章节不存在"));

        if (!chapter.getNovelId().equals(novelId)) {
            throw new BusinessException("该章节不属于此小说");
        }

        // 重新算字数差
        int oldWords = chapter.getWordCount();
        int newWords = dto.getContent().length();
        int diff = newWords - oldWords;

        // 更新章节
        chapter.setTitle(dto.getTitle());
        chapter.setContent(dto.getContent());
        chapter.setWordCount(newWords);
        chapterRepository.save(chapter);

        // 更新小说总字数
        Novel novel = novelRepository.findById(novelId).orElseThrow();
        novel.setTotalWords(novel.getTotalWords() + diff);
        novelRepository.save(novel);

        return chapter;
    }

    // ========== 4. 删除章节 ==========
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "novel", key = "#novelId")
    @Override
    public void deleteChapter(Integer novelId, Integer chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BusinessException("章节不存在"));

        if (!chapter.getNovelId().equals(novelId)) {
            throw new BusinessException("该章节不属于此小说");
        }

        // 扣减字数
        Novel novel = novelRepository.findById(novelId).orElseThrow();
        novel.setTotalWords(novel.getTotalWords() - chapter.getWordCount());
        novelRepository.save(novel);

        chapterRepository.delete(chapter);

        // 重排章节号
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        for (int i = 0; i < chapters.size(); i++) {
            chapters.get(i).setChapterNumber(i + 1);
        }
        chapterRepository.saveAll(chapters);
    }

    // ========== 5. 删除小说 ==========
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "novel", key = "#id")
    @Override
    public void deleteNovel(Integer id) {
        Novel novel = novelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("小说不存在"));

        // 先删该小说的所有章节（避免脏数据）
        chapterRepository.deleteByNovelId(id);

        // 再删小说
        novelRepository.delete(novel);

        log.info("删除小说: id={}, title={}", id, novel.getTitle());
    }

    // ========== 6. 修改小说 ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "novel", key = "#id")
    public void updateNovel(Integer id, NovelDTO dto) {
        Novel novel = novelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("小说不存在"));

        novel.setTitle(dto.getTitle());
        novel.setAuthor(dto.getAuthor());
        novel.setSummary(dto.getSummary());
        novel.setCategory(dto.getCategory());
        novel.setCoverUrl(dto.getCoverUrl());

        novelRepository.save(novel);
    }

    // ========== 7. 小说列表（分页，按更新时间倒序） ==========
    @Override
    public Page<Novel> findPage(int page, int size, String category, String status, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by("lastUpdateTime").descending());
        Specification<Novel> spec = Specification.allOf(
                NovelSpecifications.categoryEquals(category),
                NovelSpecifications.statusEquals(status),
                NovelSpecifications.titleContains(keyword)
        );
        return novelRepository.findAll(spec, pageable);
    }

    // ========== 8. 小说详情 + 章节目录 ==========
    @Transactional(readOnly = true)
    @Override
    public NovelDetailVO findDetail(Integer novelId) {
        Cache novelCache = cacheManager.getCache("novel");
        Cache nullCache = cacheManager.getCache("novel-null");

        Object nullMark = null;
        try {
            nullMark = nullCache.get(novelId);
        } catch (Exception e) {
            log.warn("空标记读取失败，降级。novelId={}, err={}", novelId, e.getMessage());
        }
        NovelDetailVO cached = null;
        try {
            cached = novelCache.get(novelId, NovelDetailVO.class);
        } catch (Exception e) {
            log.warn("详情缓存读取失败，降级。novelId={}, err={}", novelId, e.getMessage());
        }

        // 命中空值缓存：之前查过，确认不存在
        if (nullMark != null) {
            throw new BusinessException("小说不存在");
        }
        // 命中正常缓存：直接返回，不查数据库
        if (cached != null) {
            return cached;
        }

        Object lock =locks.computeIfAbsent(novelId, k -> new Object());
        synchronized (lock) {
            Object nullMark2 = null;
            try {
                nullMark2 = nullCache.get(novelId);
            } catch (Exception e) {
                log.warn("空标记读取失败，降级。novelId={}, err={}", novelId, e.getMessage());
            }
            NovelDetailVO cached2 = null;
            try {
                cached2 = novelCache.get(novelId, NovelDetailVO.class);
            } catch (Exception e) {
                log.warn("详情缓存读取失败，降级。novelId={}, err={}", novelId, e.getMessage());
            }
            if (nullMark2 != null){
                throw new BusinessException("小说不存在");
            }
            if (cached2 != null) {
                return cached2;
            }
            Optional<Novel> opt = novelRepository.findById(novelId);
            // 数据库也没有：写入空值缓存后抛异常
            if (opt.isEmpty()) {
                try {
                    nullCache.put(novelId, Boolean.TRUE);
                } catch (Exception e) {
                    log.warn("空标记写入失败，忽略。novelId={}, err={}", novelId, e.getMessage());
                }
                throw new BusinessException("小说不存在");
            }

            Novel novel = opt.get();
            List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);

            // 组装 VO
            NovelDetailVO vo = new NovelDetailVO();
            vo.setId(novel.getId());
            vo.setTitle(novel.getTitle());
            vo.setAuthor(novel.getAuthor());
            vo.setSummary(novel.getSummary());
            vo.setCoverUrl(novel.getCoverUrl());
            vo.setCategory(novel.getCategory());
            vo.setStatus(novel.getStatus());
            vo.setTotalWords(novel.getTotalWords());
            vo.setLastUpdateTime(novel.getLastUpdateTime());

            // 把 Chapter 转成 ChapterOutline（去掉 content）
            List<NovelDetailVO.ChapterOutline> outlines = chapters.stream().map(c -> {
                NovelDetailVO.ChapterOutline o = new NovelDetailVO.ChapterOutline();
                o.setId(c.getId());
                o.setChapterNumber(c.getChapterNumber());
                o.setTitle(c.getTitle());
                o.setWordCount(c.getWordCount());
                o.setCreateTime(c.getCreateTime());
                return o;
            }).toList();

            vo.setChapters(outlines);
            try {
                novelCache.put(novelId, vo);
            } catch (Exception e) {
                log.warn("详情缓存写入失败，忽略。novelId={}, err={}", novelId, e.getMessage());
            }
            return vo;
        }
    }

    // ========== 9. 阅读某一章（返回完整正文） ==========
    @Transactional(readOnly = true)
    @Override
    public Chapter readChapter(Integer novelId, Integer chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BusinessException("章节不存在"));

        if (!chapter.getNovelId().equals(novelId)) {
            throw new BusinessException("该章节不属于此小说");
        }

        // 总榜热度 +1
        stringRedisTemplate.opsForZSet().incrementScore(
                "ranking:novel:total",
                String.valueOf(novelId),
                1);

        // 日榜热度 +1
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dailyRankKey = "ranking:novel:day:" + today;
        stringRedisTemplate.opsForZSet().incrementScore(
                dailyRankKey,
                String.valueOf(novelId),
                1);
        stringRedisTemplate.expire(dailyRankKey, 2, TimeUnit.DAYS);

        return chapter;
    }

    // ========== 10. 总榜 ==========
    @Override
    public List<NovelRankVO> findRanking(int top) {
        return findRankingByKey("ranking:novel:total", top);
    }

    // ========== 11. 日榜 ==========
    @Override
    public List<NovelRankVO> findDailyRanking(int top) {
        String todayStr = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return findRankingByKey("ranking:novel:day:" + todayStr, top);
    }

    // ========== 12. 获取全部分类 ==========
    @Override
    public List<String> findAllCategories() {
        return novelRepository.findDistinctCategories();
    }

    // ========== 13. 排行榜通用逻辑 ==========
    private List<NovelRankVO> findRankingByKey(String key, int top) {
        Set<ZSetOperations.TypedTuple<String>> tuples;
        try {
            tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, 0, top - 1);
        } catch (Exception e) {
            // Redis 故障时降级：返回空列表，不抛异常
            log.warn("查询缓存失败，降级返回空列表。rankKey={}, top={}", key, top, e);
            return new ArrayList<>();
        }

        // Redis 里没有数据（刚上线）也返回空列表
        if (tuples == null || tuples.isEmpty()) {
            return new ArrayList<>();
        }

        // 准备两个容器：一个存有序的 ID 列表，一个存 ID -> 热度的映射
        List<Integer> novelIds = new ArrayList<>();     // 用来保持顺序
        Map<Integer, Double> scoreMap = new HashMap<>(); // 用来放热度

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Integer id = Integer.valueOf(tuple.getValue());
            Double score = tuple.getScore();
            novelIds.add(id);
            scoreMap.put(id, score);
        }

        // 批量查数据库，拿到所有小说的详细信息
        List<Novel> novels = novelRepository.findAllById(novelIds);
        Map<Integer, Novel> novelMap = novels.stream()
                .collect(Collectors.toMap(
                        Novel::getId,
                        Function.identity()
                ));

        // 按 novelIds 的顺序组装 VO 列表（findAllById 不保证顺序）
        List<NovelRankVO> result = new ArrayList<>();
        for (Integer id : novelIds) {
            Novel novel = novelMap.get(id);
            // 小说被删了就跳过
            if (novel == null) {
                continue;
            }

            NovelRankVO vo = new NovelRankVO();
            vo.setNovelId(id);
            vo.setTitle(novel.getTitle());
            vo.setCoverUrl(novel.getCoverUrl());
            vo.setHeat(scoreMap.get(id).longValue());

            result.add(vo);
        }
        return result;
    }
}
