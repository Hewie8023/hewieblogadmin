package com.hewie.blog.controller.admin;

import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IWebsiteInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/web_site_info")
public class WebSiteInfoAdminApi {

    @Autowired
    private IWebsiteInfoService websiteInfoService;

    @PreAuthorize("@permission.admin()")
    @GetMapping("/title")
    public ResponseResult getWebSiteTitle() {
        return websiteInfoService.getWebsiteTitle();
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/title")
    public ResponseResult updateWebSiteTitle(@RequestParam("title") String title) {
        return websiteInfoService.updateWebsiteTitle(title);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/seo")
    public ResponseResult getWebSiteSeoInfo() {
        return websiteInfoService.getSeoInfo();
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/seo")
    public ResponseResult updateWebSiteSeoInfo(@RequestParam("keywords") String keywords,
                                               @RequestParam("description") String description) {
        return websiteInfoService.updateSeoInfo(keywords, description);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/view_count")
    public ResponseResult getWebSiteViewCount() {
        return websiteInfoService.getViewCount();
    }


}
