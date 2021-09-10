package com.hewie.blog.dao;

import com.hewie.blog.pojo.HewieUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserDao extends JpaRepository<HewieUser, String>, JpaSpecificationExecutor<HewieUser> {

    /**
     * 根据用户名查找
     * @param userName
     * @return
     */
    HewieUser findOneByUserName(String userName);

    /**
     * 根据邮箱查找
     * @param email
     * @return
     */
    HewieUser findOneByEmail(String email);

    /**
     * 通过id获取用户
     * @param userId
     * @return
     */
    HewieUser findOneById(String userId);

    /**
     * 通过修改用户状态来删除
     * @param userId
     * @return
     */
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE `tb_user` SET `state` = '0' WHERE `id` = ?")
    int deleteUserByState(String userId);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE `tb_user` SET `password` = ?1 WHERE `email` = ?2")
    int updatePasswordByEmail(String newPassword, String email);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE `tb_user` SET `email` = ?1 WHERE `id` = ?2")
    int updateEmailById(String email, String id);
}
