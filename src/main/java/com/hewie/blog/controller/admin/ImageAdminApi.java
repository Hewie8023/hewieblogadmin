package com.hewie.blog.controller.admin;

import com.hewie.blog.interceptor.CheckTooFrequentCommit;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/admin/image")
public class ImageAdminApi {

    @Autowired
    private IImageService imageService;

    /**
     * 关于文件（图片）上传
     * 一般比较常用的事对象存储-->简单、花钱
     * 使用Nginx + fastDFS --> fastDFS（处理文件上传），Nginx处理文件访问 ：麻烦
     *
     *
     * @param file
     * @return
     */
    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PostMapping("/{original}")
    public ResponseResult uploadImage(@PathVariable("original") String original, @RequestParam("file")MultipartFile file) {
        return imageService.uploadImage(original, file);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{imageId}")
    public ResponseResult deleteImage(@PathVariable("imageId") String imageId) {
        return imageService.deleteImageById(imageId);
    }


    @PreAuthorize("@permission.admin()")
    @GetMapping("/{imageId}")
    public void getImage(HttpServletResponse response, @PathVariable("imageId") String imageId) {
        imageService.viewImage(response, imageId);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/list/{page}/{size}")
    public ResponseResult listImages(@PathVariable("page") int page, @PathVariable("size") int size,
                                     @RequestParam(value = "original", required = false) String original) {
        return imageService.listImages(page, size, original);
    }
}
