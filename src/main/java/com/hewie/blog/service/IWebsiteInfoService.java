package com.hewie.blog.service;

import com.hewie.blog.response.ResponseResult;

public interface IWebsiteInfoService {
    ResponseResult getWebsiteTitle();

    ResponseResult updateWebsiteTitle(String title);

    ResponseResult getSeoInfo();

    ResponseResult updateSeoInfo(String keywords, String description);

    ResponseResult getViewCount();

    void updateViewCount();


}
