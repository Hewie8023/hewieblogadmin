package com.hewie.blog.service;

import com.hewie.blog.pojo.Article;
import com.hewie.blog.response.ResponseResult;

public interface ISolrService {
    ResponseResult doSearch(String keyword, int page, int size, String categoryId, Integer sort);

    void addArticle(Article article);

    void deleteArticle(String articleId);

    void updateArticle(String articleId,Article article);

}
