package com.hewie.blog.dao;

import com.hewie.blog.pojo.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SettingsDao extends JpaRepository<Setting, String>, JpaSpecificationExecutor<Setting> {
    Setting findOneByKey(String key);
}
