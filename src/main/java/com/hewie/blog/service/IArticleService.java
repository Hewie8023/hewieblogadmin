package com.hewie.blog.service;

import com.hewie.blog.pojo.Article;
import com.hewie.blog.response.ResponseResult;

public interface IArticleService {
    ResponseResult postArticle(Article article);

    ResponseResult listArticles(int page, int size, String keyword, String categoryId, String state);

    ResponseResult getArticle(String articleId, String type);

    ResponseResult updateArticle(String articleId, Article article);

    ResponseResult deleteArticle(String articleId);

    ResponseResult deleteArticleByState(String articleId);

    ResponseResult topArticle(String articleId);

    ResponseResult listTopArticles();

    ResponseResult listRecommendArticles(String articleId, int size);

    ResponseResult listArticlesByLabel(int page, int size, String label);

    ResponseResult listLabels(int size);

    ResponseResult getArticleCount();


    ResponseResult listArticlesByUserId(int page, int size, String userId);

    ResponseResult getArticleNumByUserId(String userId);

    ResponseResult getViewNumByUserId(String userId);

    ResponseResult listTopTenArticles();

    ResponseResult listLastTenArticles();
}
