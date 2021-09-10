package com.hewie.blog.service;

import com.hewie.blog.pojo.Comment;
import com.hewie.blog.response.ResponseResult;

public interface ICommentService {
    ResponseResult postComment(Comment comment);

    ResponseResult listCommentsByArticleId(String articleId, int page, int size);

    ResponseResult deleteComment(String commentId);

    ResponseResult listComments(int page, int size);

    ResponseResult topComment(String commentId);

    ResponseResult getCommentCount();

}
