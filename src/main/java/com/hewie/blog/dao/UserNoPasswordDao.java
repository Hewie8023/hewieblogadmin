package com.hewie.blog.dao;

import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.pojo.HewieUserNoPassword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserNoPasswordDao extends JpaRepository<HewieUserNoPassword, String>, JpaSpecificationExecutor<HewieUserNoPassword> {
}
