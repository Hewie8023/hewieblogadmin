package com.hewie.blog.controller.portal;

import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ICategoryService;
import com.hewie.blog.service.IFriendLinkService;
import com.hewie.blog.service.ILooperService;
import com.hewie.blog.service.IWebsiteInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal/web_site_info")
public class WebSiteInfoPortalApi {


    @Autowired
    private IFriendLinkService friendLinkService;

    @Autowired
    private ILooperService looperService;

    @Autowired
    private IWebsiteInfoService websiteInfoService;



    @GetMapping("/title")
    public ResponseResult getWebSiteTitle() {
        return websiteInfoService.getWebsiteTitle();
    }

    @GetMapping("/view_count")
    public ResponseResult getWebSiteViewCount() {
        return websiteInfoService.getViewCount();
    }

    @GetMapping("/seo")
    public ResponseResult getWebSiteSeoInfo() {
        return websiteInfoService.getSeoInfo();
    }

    @GetMapping("/loop")
    public ResponseResult getLoops() {
        return looperService.listLoops();
    }

    @GetMapping("/friend_link")
    public ResponseResult getFriendLinks() {
        return friendLinkService.listFriendLinks();
    }

    /**
     * 统计访问页，每个页面统计一次，pv：page view
     * 直接增加一个访问量，可以刷量
     *
     * 根据ip进行过滤
     *
     * 统计信息，通过redis进行统计，数据也会保存在mysql
     * 不会每次都更新到mysql里，当用户去获取访问量时，会更新一次
     * 平时的调用，只增加redis里的访问量
     *
     * redis时机：每个页面访问的时候，如果不在，从mysql里读取数据，写到redis里
     * 如果在，就自增
     *
     * mysql时机：用户读取网站总访问量的时候，就读取redis里的，并且更新到mysql里
     * 如果redis里没有，就读取mysql写到redis里
     */
    @PutMapping("/view_count")
    public void updateViewCount() {
        websiteInfoService.updateViewCount();
    }
}
