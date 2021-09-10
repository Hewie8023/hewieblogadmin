package com.hewie.blog.service.impl;

import com.hewie.blog.dao.SettingsDao;
import com.hewie.blog.pojo.Setting;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IWebsiteInfoService;
import com.hewie.blog.utils.Constants;
import com.hewie.blog.utils.RedisUtil;
import com.hewie.blog.utils.SnowflakeIdWorker;
import com.hewie.blog.utils.TextUtils;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class WebsiteInfoServiceImpl extends BaseService implements IWebsiteInfoService {

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private SettingsDao settingsDao;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public ResponseResult getWebsiteTitle() {
        Setting title = settingsDao.findOneByKey(Constants.Settings.WEBSITE_TITLE);
        if (title == null) {
            return ResponseResult.FAILED("网站标题不存在");
        }
        return ResponseResult.SUCCESS("获取网站标题成功").setData(title);
    }

    @Override
    public ResponseResult updateWebsiteTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            return ResponseResult.FAILED("网站标题不能为空");
        }
        Setting titleFromDb = settingsDao.findOneByKey(Constants.Settings.WEBSITE_TITLE);
        if (titleFromDb == null) {
            titleFromDb = new Setting();
            titleFromDb.setId(idWorker.nextId() + "");
            titleFromDb.setCreateTime(new Date());
            titleFromDb.setKey(Constants.Settings.WEBSITE_TITLE);
        }
        titleFromDb.setValue(title);
        titleFromDb.setUpdateTime(new Date());
        settingsDao.save(titleFromDb);
        return ResponseResult.SUCCESS("修改网站标题成功").setData(titleFromDb);
    }

    @Override
    public ResponseResult getSeoInfo() {
        Setting description = settingsDao.findOneByKey(Constants.Settings.WEBSITE_DESCRIPTION);
        Setting keyWords = settingsDao.findOneByKey(Constants.Settings.WEBSITE_KEYWORDS);
        Map<String, String> result = new HashMap<>();
        result.put(description.getKey(), description.getValue());
        result.put(keyWords.getKey(), keyWords.getValue());
        return ResponseResult.SUCCESS("获取网站SEO信息成功").setData(result);
    }

    @Override
    public ResponseResult updateSeoInfo(String keywords, String description) {

        if (TextUtils.isEmpty(keywords)) {
            return ResponseResult.FAILED("关键词不能为空");
        }
        if (TextUtils.isEmpty(description)) {
            return ResponseResult.FAILED("描述不能为空");
        }

        Setting descriptionFromDb = settingsDao.findOneByKey(Constants.Settings.WEBSITE_DESCRIPTION);
        if (descriptionFromDb == null) {
            descriptionFromDb = new Setting();
            descriptionFromDb.setId(idWorker.nextId() + "");
            descriptionFromDb.setKey(Constants.Settings.WEBSITE_DESCRIPTION);
            descriptionFromDb.setCreateTime(new Date());
        }
        descriptionFromDb.setValue(description);
        descriptionFromDb.setUpdateTime(new Date());
        settingsDao.save(descriptionFromDb);
        Setting keyWordsFromDb = settingsDao.findOneByKey(Constants.Settings.WEBSITE_KEYWORDS);
        if (keyWordsFromDb == null) {
            keyWordsFromDb = new Setting();
            keyWordsFromDb.setId(idWorker.nextId() + "");
            keyWordsFromDb.setKey(Constants.Settings.WEBSITE_KEYWORDS);
            keyWordsFromDb.setCreateTime(new Date());
        }
        keyWordsFromDb.setValue(keywords);
        keyWordsFromDb.setUpdateTime(new Date());
        settingsDao.save(keyWordsFromDb);
        return ResponseResult.SUCCESS("更新seo信息成功");
    }

    /**
     * 这个是全网站的访问量
     * 若要做的细一点，需要获取来源
     * 这里只统计浏览量，只统计文章
     * @return
     */
    @Override
    public ResponseResult getViewCount() {
        //先从redis获取
        String viewCountStr = (String) redisUtil.get(Constants.Settings.WEBSITE_VIEW_COUNT);
        Setting viewCount = settingsDao.findOneByKey(Constants.Settings.WEBSITE_VIEW_COUNT);

        if (viewCount == null) {
            viewCount = initViewCount();
        }
        if (TextUtils.isEmpty(viewCountStr)) {
            viewCountStr = viewCount.getValue();
            redisUtil.set(Constants.Settings.WEBSITE_VIEW_COUNT, viewCountStr);
        } else {
            viewCount.setValue(viewCountStr);
            viewCount.setUpdateTime(new Date());
            settingsDao.save(viewCount);
        }
        Map<String, Integer> result = new HashMap<>();
        result.put(viewCount.getKey(), Integer.parseInt(viewCount.getValue()));
        return ResponseResult.SUCCESS("获取浏览量成功").setData(result);
    }

    private Setting initViewCount() {
        Setting viewCount;
        viewCount = new Setting();
        viewCount.setId(idWorker.nextId() + "");
        viewCount.setKey(Constants.Settings.WEBSITE_VIEW_COUNT);
        viewCount.setValue("1");
        viewCount.setUpdateTime(new Date());
        viewCount.setCreateTime(new Date());
        settingsDao.save(viewCount);
        return viewCount;
    }

    @Override
    public void updateViewCount() {
        //redis的更新时机
        Object viewCount = redisUtil.get(Constants.Settings.WEBSITE_VIEW_COUNT);
        if (viewCount == null) {
            Setting viewCountSetting = settingsDao.findOneByKey(Constants.Settings.WEBSITE_VIEW_COUNT);
            if (viewCountSetting == null) {
                viewCountSetting = initViewCount();
            }
            redisUtil.set(Constants.Settings.WEBSITE_VIEW_COUNT, viewCountSetting.getValue());
        } else {
            //数字自增
            redisUtil.incr(Constants.Settings.WEBSITE_VIEW_COUNT, 1);
        }

    }
}
