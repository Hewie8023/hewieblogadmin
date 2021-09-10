package com.hewie.blog.controller.portal;

import com.hewie.blog.response.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/portal/app")
public class AppApi {

    /**
     * 给第三方扫描下载app
     * @return
     */
    @GetMapping("/{code}")
    public void downloadApp(@PathVariable("code") String code, HttpServletRequest request, HttpServletResponse response) {
        //todo：直接开始下载
    }
}
