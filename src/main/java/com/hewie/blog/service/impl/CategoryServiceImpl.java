package com.hewie.blog.service.impl;

import com.hewie.blog.dao.CategoryDao;
import com.hewie.blog.pojo.Category;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ICategoryService;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.Constants;
import com.hewie.blog.utils.SnowflakeIdWorker;
import com.hewie.blog.utils.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CategoryServiceImpl extends BaseService implements ICategoryService {

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private IUserService userService;

    @Override
    public ResponseResult addCategory(Category category) {
        //检查数据：名称、pinyin、描述
        if (TextUtils.isEmpty(category.getName())) {
            return ResponseResult.FAILED("分类名称不能为空");
        }
        if (TextUtils.isEmpty(category.getPinyin())) {
            return ResponseResult.FAILED("分类拼音不能为空");
        }
        if (TextUtils.isEmpty(category.getDescription())) {
            return ResponseResult.FAILED("分类描述不能为空");
        }

        //补全数据
        category.setId(idWorker.nextId() + "");
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());

        //保存数据
        categoryDao.save(category);
        //返回结果
        return ResponseResult.SUCCESS("添加分类成功").setData(category);
    }

    /**
     * 获取类别
     * @param categoryId
     * @return
     */
    @Override
    public ResponseResult getCategoryById(String categoryId) {
        Category categoryFromDb = categoryDao.findOneById(categoryId);
        if (categoryFromDb == null) {
            return ResponseResult.FAILED("获取类别失败");
        }

        return ResponseResult.SUCCESS("获取类别成功").setData(categoryFromDb);
    }

    /**
     * 获取类别列表
     * @return
     */
    @Override
    public ResponseResult listCategories() {
        Sort sort = new Sort(Sort.Direction.DESC,  "order", "createTime");

        //判断用户角色：普通用户或者未登录用户，只能获取正常的category，管理员账户可以拿到所有
        HewieUser hewieUser = userService.checkHewieUser();
        List<Category> categories;
        if (hewieUser == null || !Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
            //正常的category
            categories = categoryDao.listCategoriesByState("1");

        } else {
            categories = categoryDao.findAll(sort);
        }
        return ResponseResult.SUCCESS("获取分类列表成功").setData(categories);
    }

    /**
     * 更新分类
     * @param categoryId
     * @param category
     * @return
     */
    @Override
    public ResponseResult updateCategotyById(String categoryId, Category category) {
        Category categoryFromDb = categoryDao.findOneById(categoryId);
        if (categoryFromDb == null) {
            return ResponseResult.FAILED("分类不存在");
        }
        String name = category.getName();
        if (!TextUtils.isEmpty(name)) {
            categoryFromDb.setName(name);
        }
        String pinyin = category.getPinyin();
        if (!TextUtils.isEmpty(pinyin)) {
            categoryFromDb.setPinyin(pinyin);
        }
        String description = category.getDescription();
        if (!TextUtils.isEmpty(description)) {
            categoryFromDb.setDescription(description);
        }
        categoryFromDb.setStatus(category.getStatus());
        categoryFromDb.setOrder(category.getOrder());
        categoryFromDb.setUpdateTime(new Date());
        categoryDao.save(categoryFromDb);
        return ResponseResult.SUCCESS("修改分类成功").setData(categoryFromDb);
    }

    /**
     * 删除分类：修改状态
     * @param categoryId
     * @return
     */
    @Override
    public ResponseResult deleteCategory(String categoryId) {
        int result = categoryDao.deleteCategoryByStatus(categoryId);
        return result > 0 ? ResponseResult.SUCCESS("删除分类成功") : ResponseResult.SUCCESS("删除分类失败");
    }
}
