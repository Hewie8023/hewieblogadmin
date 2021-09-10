package com.hewie.blog.controller.admin;

import com.hewie.blog.interceptor.CheckTooFrequentCommit;
import com.hewie.blog.pojo.Category;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理中心：分类的API
 */
@RestController
@RequestMapping("/admin/category")
public class CategoryAdminApi {

    @Autowired
    private ICategoryService categoryService;

    /**
     * 添加分类
     * @param category
     * @return
     */
    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PostMapping
    public ResponseResult addCategory(@RequestBody Category category) {

        return categoryService.addCategory(category);
    }

    /**
     * 删除分类
     * @param categoryId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{categoryId}")
    public ResponseResult deleteCategory(@PathVariable("categoryId") String categoryId) {

        return categoryService.deleteCategory(categoryId);
    }

    /**
     * 修改分类
     * @param categoryId
     * @param category
     * @return
     */
    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PutMapping("/{categoryId}")
    public ResponseResult updateCategory(@PathVariable("categoryId") String categoryId, @RequestBody Category category) {
        return categoryService.updateCategotyById(categoryId, category);
    }

    /**
     * 获取类别
     *
     * 修改的时候获取一下，填充弹窗
     *
     * @param categoryId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @GetMapping("/{categoryId}")
    public ResponseResult getCategory(@PathVariable("categoryId") String categoryId) {

        return categoryService.getCategoryById(categoryId);
    }

    /**
     * 获取所有分类
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @GetMapping("/list")
    public ResponseResult listCategories() {

        return categoryService.listCategories();
    }
}
