package com.hewie.blog.dao;

import com.hewie.blog.pojo.Looper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LooperDao extends JpaRepository<Looper, String>, JpaSpecificationExecutor<Looper> {
    Looper findOneById(String loopId);

    @Query(nativeQuery = true, value = "SELECT * FROM `tb_looper` WHERE `state` = ? ORDER BY `create_time` DESC")
    List<Looper> listLoopsByState(String state);
}
