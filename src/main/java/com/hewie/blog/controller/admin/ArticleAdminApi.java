package com.hewie.blog.controller.admin;

import com.hewie.blog.interceptor.CheckTooFrequentCommit;
import com.hewie.blog.pojo.Article;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/article")
public class ArticleAdminApi {

    @Autowired
    private IArticleService articleService;

    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin() || @permission.loginUser()")
    @PostMapping
    public ResponseResult postArticle(@RequestBody Article article) {
        return articleService.postArticle(article);
    }

    /**
     * 如果是多用户：用户删除只是修改状态，管理员可以删
     * @param articleId
     * @return
     */
    @PreAuthorize("@permission.admin() || @permission.loginUser()")
    @DeleteMapping("/{articleId}")
    public ResponseResult deleteArticle(@PathVariable("articleId") String articleId) {
        return articleService.deleteArticle(articleId);
    }

    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin() || @permission.loginUser()")
    @PutMapping("/{articleId}")
    public ResponseResult updateArticle(@PathVariable("articleId") String articleId,
                                        @RequestBody Article article) {
        return articleService.updateArticle(articleId, article);
    }

    @PreAuthorize("@permission.admin()  || @permission.loginUser()")
    @GetMapping("/{articleId}")
    public ResponseResult getArticle(@PathVariable("articleId") String articleId) {
        return articleService.getArticle(articleId, "admin");
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/list/{page}/{size}")
    public ResponseResult listArticles(@PathVariable("page") int page,
                                       @PathVariable("size") int size,
                                       @RequestParam(value = "keyword", required = false) String keyword,
                                       @RequestParam(value = "categoryId", required = false) String categoryId,
                                       @RequestParam(value = "state", required = false) String state) {
        return articleService.listArticles(page, size, keyword, categoryId, state);
    }

    @PreAuthorize("@permission.admin()|| @permission.loginUser()")
    @DeleteMapping("/state/{articleId}")
    public ResponseResult deleteArticleStateByState(@PathVariable("articleId") String articleId) {
        return articleService.deleteArticleByState(articleId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/top/{articleId}")
    public ResponseResult topArticle(@PathVariable("articleId") String articleId) {
        return articleService.topArticle(articleId);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/count")
    public ResponseResult getArticleCount() {
        return articleService.getArticleCount();
    }
}
