package com.hewie.blog.controller.admin;

import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ICommentResService;
import com.hewie.blog.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comment")
public class CommentAdminApi {

    @Autowired
    private ICommentService commentService;

    @Autowired
    private ICommentResService commentResService;

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{commentId}")
    public ResponseResult deleteComment(@PathVariable("commentId") String commentId) {
        return commentService.deleteComment(commentId);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/list/{page}/{size}")
    public ResponseResult listComments(@PathVariable("page") int page, @PathVariable("size") int size) {
        return commentService.listComments(page, size);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/top/{commentId}")
    public ResponseResult topComment(@PathVariable("commentId") String commentId) {
        return commentService.topComment(commentId);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/count")
    public ResponseResult getCommentCount() {
        return commentService.getCommentCount();
    }

    @GetMapping("/list/comment/{page}/{size}")
    public ResponseResult listAllComments(@PathVariable("page") int page,
                                                     @PathVariable("size") int size){
        return commentResService.listAllComments(page, size);
    }

    @GetMapping("/list/replay/{page}/{size}")
    public ResponseResult listAllReplays(@PathVariable("page") int page,
                                          @PathVariable("size") int size){
        return commentResService.listAllReplays(page, size);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/review/{id}")
    public ResponseResult reviewCommentOrReplay(@PathVariable("id") String id) {
        return commentResService.reviewCommentOrReplay(id);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/2/{id}")
    public ResponseResult deleteCommentOrReplay(@PathVariable("id") String id){
        return commentResService.deleteCommentOrReplay(id);
    }
}
