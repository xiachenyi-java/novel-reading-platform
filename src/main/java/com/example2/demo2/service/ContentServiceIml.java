package com.example2.demo2.service;

import com.example2.demo2.common.exception.BusinessException;
import com.example2.demo2.entity.Content;
import com.example2.demo2.entity.Novel;
import com.example2.demo2.entity.User;
import com.example2.demo2.enums.Polarity;
import com.example2.demo2.repository.ContentRepository;
import com.example2.demo2.repository.ContentSpecifications;
import com.example2.demo2.repository.NovelRepository;
import com.example2.demo2.repository.UserRepository;
import com.example2.demo2.service.Iservice.IContentService;
import com.example2.demo2.vo.ContentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 夏辰义
 * 2026/10/2712:14
 */
@Service
@RequiredArgsConstructor
public class ContentServiceIml implements IContentService {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final NovelRepository novelRepository;

    //内容流显示
    @Override
    public Page<ContentVO> findPageContents(int page, int size,
                                            String polarityStr, Integer novelId,
                                            Integer userId, String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by("createTime").descending());

        //处理非法极性
        Polarity polarity = null;
        if (StringUtils.hasText(polarityStr)) {
            try {
                polarity = Polarity.valueOf(polarityStr);
            } catch (IllegalArgumentException e) {
                // 传了 INVALID，转枚举报错，直接返回空的分页
                return Page.empty(pageable);
            }
        }

        //2. 组装查询条件
        Specification<Content> spec = Specification.allOf(
                ContentSpecifications.notDeleted(),
                ContentSpecifications.polarityEquals(polarity),
                ContentSpecifications.novelIdEquals(novelId),
                ContentSpecifications.authorIdEquals(userId), // 注意：传进来叫userId，查的是authorId
                ContentSpecifications.keywordContains(keyword)
        );

        //3. 查数据库，拿到第一层（内容本身）
        Page<Content> contentPage = contentRepository.findAll(spec, pageable);

        //如果是空就不查了
        if (contentPage.isEmpty()){
            return contentPage.map(c -> null);
        }

        // 4. 提取这一页所有的 authorId 和 novelId（去重）
        Set<Integer> authorIds = contentPage.getContent().stream()
                .map(Content::getAuthorId)
                .collect(Collectors.toSet());

        Set<Integer> novelIds = contentPage.getContent().stream()
                .map(Content::getNovelId)
                .collect(Collectors.toSet());

        //5.批量查库，转化成map
        Map<Integer, User> userMap = userRepository.findAllById(authorIds)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Integer, Novel> novelMap = novelRepository.findAllById(novelIds)
                .stream().collect(Collectors.toMap(Novel::getId, Function.identity()));

        // 6. 组装 VO
        return contentPage.map(content -> {
            ContentVO vo = new ContentVO();
            //拷贝
            vo.setId(content.getId());
            vo.setPolarity(content.getPolarity());
            vo.setGrade(content.getGrade());
            vo.setTitle(content.getTitle());
            vo.setText(content.getBody());   // 注意这里 body 转 text
            vo.setCreateTime(content.getCreateTime());

            // 组装作者信息 AuthorBrief
            User author = userMap.get(content.getAuthorId());
            if (author != null) {
                ContentVO.AuthorBrief authorBrief = new ContentVO.AuthorBrief();
                authorBrief.setId(author.getId());
                authorBrief.setUsername(author.getUsername());
                // isAi 数据库是 0/1，VO 是 Boolean
                authorBrief.setIsAi(author.getIsAi() != null && author.getIsAi() == 1);
                vo.setAuthor(authorBrief);
            }

            // 组装书籍信息 NovelBrief (照着上面的作者信息写)
            Novel novel = novelMap.get(content.getNovelId());
            if (novel != null) {
                ContentVO.NovelBrief novelBrief = new ContentVO.NovelBrief();
                novelBrief.setId(novel.getId());
                novelBrief.setTitle(novel.getTitle());
                novelBrief.setAuthor(novel.getAuthor());
                novelBrief.setCategory(novel.getCategory());
                novelBrief.setCoverUrl(novel.getCoverUrl());

                vo.setNovel(novelBrief);
            }

            return vo;
        });

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Integer id) {
        Content content = contentRepository.findByIdAndDeleted(id,0).
                orElseThrow(() -> new BusinessException("书评不存在"));

        content.setDeleted(1);
        contentRepository.save(content);
    }
}
