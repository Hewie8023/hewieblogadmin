package com.hewie.blog.service;

import com.hewie.blog.pojo.Firstcomment;
import com.hewie.blog.pojo.Replay;
import com.hewie.blog.response.ResponseResult;

public interface ICommentResService {
    ResponseResult postFirstComment(Firstcomment firstcomment);

    ResponseResult postReplay(Replay replay);

    ResponseResult listAllCommentsByArticleId(String articleId, int page, int size);

    ResponseResult reviewCommentOrReplay(String id);

    ResponseResult deleteCommentOrReplay(String id);

    ResponseResult listAllComments(int page, int size);

    ResponseResult listAllReplays(int page, int size);

}
