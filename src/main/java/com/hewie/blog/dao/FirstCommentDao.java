package com.hewie.blog.dao;

import com.hewie.blog.pojo.Firstcomment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface FirstCommentDao extends JpaRepository<Firstcomment, String>, JpaSpecificationExecutor<Firstcomment> {

    Page<Firstcomment> findAllByArticleId(String articleId, Pageable pageable);

    Firstcomment findOneById(String id);

}

