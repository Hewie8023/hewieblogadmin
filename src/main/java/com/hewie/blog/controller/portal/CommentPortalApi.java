package com.hewie.blog.controller.portal;

import com.hewie.blog.interceptor.CheckTooFrequentCommit;
import com.hewie.blog.pojo.Comment;
import com.hewie.blog.pojo.Firstcomment;
import com.hewie.blog.pojo.Replay;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ICommentResService;
import com.hewie.blog.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portal/comment")
public class CommentPortalApi {

    @Autowired
    private ICommentService commentService;

    @Autowired
    private ICommentResService commentResService;

    @CheckTooFrequentCommit
    @PostMapping
    public ResponseResult postComment(@RequestBody Comment comment) {
        return commentService.postComment(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseResult deleteComment(@PathVariable("commentId") String commentId) {
        return commentService.deleteComment(commentId);
    }

    @GetMapping("/list/{articleId}/{page}/{size}")
    public ResponseResult listCommentsByArticleId(@PathVariable("articleId") String articleId,
                                                  @PathVariable("page") int page,
                                                  @PathVariable("size") int size) {
        return commentService.listCommentsByArticleId(articleId, page, size);
    }

    @CheckTooFrequentCommit
    @PostMapping("/first-comment")
    public ResponseResult postFirstComment(@RequestBody Firstcomment firstcomment) {
        return commentResService.postFirstComment(firstcomment);
    }

    @CheckTooFrequentCommit
    @PostMapping("/replay")
    public ResponseResult postReplay(@RequestBody Replay replay) {
        return commentResService.postReplay(replay);
    }

    @GetMapping("/list/2/{articleId}/{page}/{size}")
    public ResponseResult listAllCommentsByArticleId(@PathVariable("articleId") String articleId,
                                                     @PathVariable("page") int page,
                                                     @PathVariable("size") int size){
        return commentResService.listAllCommentsByArticleId(articleId, page, size);
    }


    @DeleteMapping("/2/{id}")
    public ResponseResult deleteCommentOrReplay(@PathVariable("id") String id){
        return commentResService.deleteCommentOrReplay(id);
    }
}
