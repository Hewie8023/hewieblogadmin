package com.hewie.blog.dao;

import com.hewie.blog.pojo.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ArticleDao extends JpaRepository<Article, String>, JpaSpecificationExecutor<Article> {
    Article findOneById(String articleId);

    @Modifying
    int deleteAllById(String articleId);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE `tb_article` SET `state` = '0' WHERE `id` = ?")
    int deleteArticleByState(String articleId);

    @Query(nativeQuery = true, value = "select `labels` from `tb_article` where `id` = ?")
    String listArticlesById(String articleId);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM `tb_article` WHERE `user_id`=?")
    int getArticleNumByUserId(String userId);

    @Query(nativeQuery = true, value = "SELECT SUM(`view_count`) AS  allNum FROM `tb_article` WHERE `user_id`=?")
    int getViewNumByUserId(String userId);
}
