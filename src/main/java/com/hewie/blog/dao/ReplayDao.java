package com.hewie.blog.dao;

import com.hewie.blog.pojo.Replay;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReplayDao  extends JpaRepository<Replay, String>, JpaSpecificationExecutor<Replay> {
    List<Replay> findAllByFatherCommentId(String commentId, Sort sort);

    Replay findOneById(String id);

}
