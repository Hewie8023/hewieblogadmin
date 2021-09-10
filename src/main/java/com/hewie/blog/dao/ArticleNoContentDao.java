package com.hewie.blog.dao;

import com.hewie.blog.pojo.Article;
import com.hewie.blog.pojo.ArticleNoContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArticleNoContentDao extends JpaRepository<ArticleNoContent, String>, JpaSpecificationExecutor<ArticleNoContent> {
    ArticleNoContent findOneById(String articleId);

    @Query(nativeQuery = true, value = "SELECT * FROM `tb_article` WHERE `labels` LIKE ? AND `id` AND (`state` = '1' || `state` = '3') != ? LIMIT ?")
    List<ArticleNoContent> listArticleByLabel(String targetLabel, String originalArticleId, int size);

    @Query(nativeQuery = true, value = "SELECT * FROM `tb_article` WHERE `id` != ? AND (`state` = '1' || `state` = '3') ORDER BY `create_time` DESC   LIMIT ?")
    List<ArticleNoContent> listLastedArticleBySize(String originalArticleId, int dxSize);
}
