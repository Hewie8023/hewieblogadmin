package com.hewie.blog.service;

import com.hewie.blog.response.ResponseResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IImageService {
    ResponseResult uploadImage(String original, MultipartFile file);

    void viewImage(HttpServletResponse response, String imageId);

    ResponseResult listImages(int page, int size, String original);

    ResponseResult deleteImageById(String imageId);

    void createQrCode(String code, HttpServletResponse response, HttpServletRequest request);

}
