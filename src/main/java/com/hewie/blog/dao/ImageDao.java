package com.hewie.blog.dao;

import com.hewie.blog.pojo.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ImageDao extends JpaRepository<Image, String>, JpaSpecificationExecutor<Image> {

    @Modifying
    @Query(nativeQuery = true, value = "update `tb_images` set `state` = '0' where `id` = ?")
    int deleteImageByState(String imageId);
}
