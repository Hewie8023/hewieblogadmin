package com.hewie.blog.controller.portal;

import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/portal/image")
public class ImagePortalApi {

    @Autowired
    private IImageService imageService;

    @GetMapping("/{imageId}")
    public void getImage(HttpServletResponse response, @PathVariable("imageId") String imageId) {
        imageService.viewImage(response, imageId);
    }

    @GetMapping("/qr-code/{code}")
    public void getQrCodeImage(@PathVariable("code") String code, HttpServletResponse response, HttpServletRequest request) {
        //生成二维码
        imageService.createQrCode(code, response, request);
    }
}
