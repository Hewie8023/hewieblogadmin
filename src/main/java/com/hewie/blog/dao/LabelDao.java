package com.hewie.blog.dao;

import com.hewie.blog.pojo.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LabelDao extends JpaRepository<Label, String>, JpaSpecificationExecutor<Label> {

    @Modifying
    int deleteOneById(String id);

    /**
     * 根据id查找一个
     * @param id
     * @return
     */
    Label findOneById(String id);

    Label findOneByName(String name);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE `tb_labels`  SET `count` = `count` + 1 WHERE `name` = ?")
    int updateCountByName(String labelName);
}
