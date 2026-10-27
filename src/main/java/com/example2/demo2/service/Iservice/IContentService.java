package com.example2.demo2.service.Iservice;

import com.example2.demo2.vo.ContentVO;
import org.springframework.data.domain.Page;

public interface IContentService {

    //1.评论列表
    Page<ContentVO> findPageContents(int page, int size,
                                     String polarityStr, Integer novelId,
                                     Integer userId, String keyword);

    //软删
    void softDelete(Integer id);
}
