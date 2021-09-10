package com.hewie.blog.dao;

import com.hewie.blog.pojo.FriendLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FriendLinkDao extends JpaRepository<FriendLink, String>, JpaSpecificationExecutor<FriendLink> {
    FriendLink findOneById(String friendLinkId);

    int deleteAllById(String friendLinkId);

    @Query(nativeQuery = true, value = "SELECT * FROM `tb_friends` WHERE `state` = ? ORDER BY `create_time` DESC")
    List<FriendLink> listFriendLinksByState(String state);

}
