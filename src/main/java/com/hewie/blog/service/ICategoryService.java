package com.hewie.blog.service;

import com.hewie.blog.pojo.Category;
import com.hewie.blog.response.ResponseResult;

public interface ICategoryService {
    ResponseResult addCategory(Category category);

    ResponseResult getCategoryById(String categoryId);

    ResponseResult listCategories();

    ResponseResult updateCategotyById(String categoryId, Category category);

    ResponseResult deleteCategory(String categoryId);

}
