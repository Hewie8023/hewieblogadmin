package com.hewie.blog.controller.portal;

import com.hewie.blog.interceptor.CheckTooFrequentCommit;
import com.hewie.blog.pojo.Article;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IArticleService;
import com.hewie.blog.service.ICategoryService;
import com.hewie.blog.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portal/article")
public class ArticlePortalApi {

    @Autowired
    private IArticleService articleService;


    @Autowired
    private ICategoryService categoryService;

    @GetMapping("/list/{page}/{size}")
    public ResponseResult listArticles(@PathVariable("page") int page, @PathVariable("size") int size) {
        return articleService.listArticles(page, size, null, null, Constants.Article.STATE_PUBLISH);
    }

    @GetMapping("/list/top")
    public ResponseResult getTopArticles() {
        return articleService.listTopArticles();
    }

    @GetMapping("/list/{categoryId}/{page}/{size}")
    public ResponseResult listArticlesByCategoryId(@PathVariable("categoryId") String categoryId,
                                                 @PathVariable("page") int page,
                                                 @PathVariable("size") int size) {
        return articleService.listArticles(page, size, null, categoryId, Constants.Article.STATE_PUBLISH);
    }

    @GetMapping("/list/byUid/{userId}/{page}/{size}")
    public ResponseResult listArticlesByUserId(@PathVariable("userId") String userId,
                                               @PathVariable("page") int page,
                                               @PathVariable("size") int size){
        return articleService.listArticlesByUserId(page, size, userId);
    }

    /**
     * 获取文章详情
     * 权限：任意用户
     * 内容过滤：只允许拿置顶的或者正常发布的
     * 草稿和删除的需要权限
     * @param articleId
     * @return
     */
    @GetMapping("/{articleId}")
    public ResponseResult getArticleDetail(@PathVariable("articleId") String articleId) {
        return articleService.getArticle(articleId, "portal");
    }

    /**
     * 通过文章的标签来计算匹配度
     * 标签：一个或多个（5个内）
     * 每次随机取一个标签，获取推荐的文章
     * 如果没有相关文章，则从数据中获取最新的文章
     * @param articleId
     * @return
     */
    @GetMapping("/recommend/{articleId}/{size}")
    public ResponseResult getRecommendArticles(@PathVariable("articleId") String articleId,
                                               @PathVariable("size") int size) {
        return articleService.listRecommendArticles(articleId, size);
    }

    /**
     * 获取标签云
     * 用户点击标签，就会通过标签获取相关的文章列表
     * @param size
     * @return
     */
    @GetMapping("/label/{size}")
    public ResponseResult getLabels(@PathVariable("size") int size) {
        return articleService.listLabels(size);
    }

    @GetMapping("/list/label/{label}/{page}/{size}")
    public ResponseResult listArticlesByCategoryLabel(@PathVariable("label") String label,
                                                   @PathVariable("page") int page,
                                                   @PathVariable("size") int size) {
        return articleService.listArticlesByLabel(page, size, label);
    }


    @GetMapping("/categories")
    public ResponseResult getCategories() {
        return categoryService.listCategories();
    }
}
