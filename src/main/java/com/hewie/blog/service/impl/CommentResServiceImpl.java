package com.hewie.blog.service.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.hewie.blog.dao.ArticleNoContentDao;
import com.hewie.blog.dao.FirstCommentDao;
import com.hewie.blog.dao.ReplayDao;
import com.hewie.blog.dao.UserDao;
import com.hewie.blog.pojo.*;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ICommentResService;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.Constants;
import com.hewie.blog.utils.RedisUtil;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CommentResServiceImpl extends BaseService implements ICommentResService {

    @Autowired
    private FirstCommentDao firstCommentDao;

    @Autowired
    private IUserService userService;

    @Autowired
    private ArticleNoContentDao articleNoContentDao;

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ReplayDao replayDao;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private Gson gson;

    @Override
    public ResponseResult postFirstComment(Firstcomment firstcomment) {
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }

        //检查内容
        String articleId = firstcomment.getArticleId();
        if (TextUtils.isEmpty(articleId)) {
            return ResponseResult.FAILED("文章ID不能为空");
        }
        ArticleNoContent articleFromDb = articleNoContentDao.findOneById(articleId);
        if (articleFromDb == null) {
            return ResponseResult.FAILED("文章不存在");
        }
        String content = firstcomment.getContent();
        if (TextUtils.isEmpty(content)) {
            return ResponseResult.FAILED("评论内容不能为空");
        }

        firstcomment.setId(idWorker.nextId()+"");
        firstcomment.setUserId(hewieUser.getId());
        firstcomment.setUserName(hewieUser.getUserName());
        firstcomment.setUserAvatar(hewieUser.getAvatar());
        firstcomment.setCreateTime(new Date());
        firstcomment.setUpdateTime(new Date());

        firstCommentDao.save(firstcomment);

        redisUtil.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE + firstcomment.getArticleId());

        return ResponseResult.SUCCESS("添加评论成功");
    }

    @Override
    public ResponseResult postReplay(Replay replay) {
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }

        //检查内容
        String articleId = replay.getArticleId();
        if (TextUtils.isEmpty(articleId)) {
            return ResponseResult.FAILED("文章ID不能为空");
        }

        String commentId = replay.getFatherCommentId();
        if (TextUtils.isEmpty(commentId)) {
            return ResponseResult.FAILED("父评论id不能为空");
        }
        String content = replay.getContent();
        if (TextUtils.isEmpty(content)) {
            return ResponseResult.FAILED("回复内容不能为空");
        }
        String toUid = replay.getToUid();
        if (TextUtils.isEmpty(toUid)) {
            return ResponseResult.FAILED("回复对象不能为空");
        }

        replay.setId(idWorker.nextId()+"");
        replay.setFromUid(hewieUser.getId());
        replay.setFromUname(hewieUser.getUserName());
        replay.setFromUavatar(hewieUser.getAvatar());
        HewieUser toUser = userDao.getOne(toUid);
        replay.setToUname(toUser.getUserName());
        replay.setToUavatar(toUser.getAvatar());
        replay.setCreateTime(new Date());
        replay.setUpdateTime(new Date());

        replayDao.save(replay);

        redisUtil.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE + replay.getArticleId());
        return ResponseResult.SUCCESS("添加回复成功");
    }

    @Override
    public ResponseResult listAllCommentsByArticleId(String articleId, int page, int size) {
        page = checkPage(page);
        size = checkSize(size);

        if (page == 1) {

            //如果是第一页，先从缓存中拿
            //如果没有，往下走
            String commentCacheJson = (String) redisUtil.get(Constants.Comment.KEY_COMMENT_FIRST_PAGE + articleId);
            if (!TextUtils.isEmpty(commentCacheJson)) {
                PageList<CommentRes> result = gson.fromJson(commentCacheJson, new TypeToken<PageList<CommentRes>>(){}.getType());
                return ResponseResult.SUCCESS("获取文章评论列表成功").setData(result);
            }
        }

        Sort sort = new Sort(Sort.Direction.DESC, "state","createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Firstcomment> all = firstCommentDao.findAllByArticleId(articleId, pageable);
        List<CommentRes> commentResList = new ArrayList<>();
        for (Firstcomment firstcomment : all) {
            String commentId = firstcomment.getId();
            CommentRes res = new CommentRes();
            res.setFirstcomment(firstcomment);
            if (!TextUtils.isEmpty(commentId)) {
                Sort sort1 = new Sort(Sort.Direction.ASC, "state","createTime");
                List<Replay> replayList = replayDao.findAllByFatherCommentId(commentId, sort1);
                res.setReplayList(replayList);
            }
            commentResList.add(res);
        }
        PageList<Firstcomment> result = new PageList<>();
        result.parsePage(all);
        result.parseComments(commentResList);

        //保存一份到缓存
        if (page == 1) {
            redisUtil.set(Constants.Comment.KEY_COMMENT_FIRST_PAGE + articleId, gson.toJson(result), Constants.TimeValueInSecond.HOUR);
        }
        return ResponseResult.SUCCESS("获取文章评论列表成功").setData(result);
    }

    @Override
    public ResponseResult reviewCommentOrReplay(String id) {
        Firstcomment firstcomment = firstCommentDao.findOneById(id);
        Replay replay;
        if (firstcomment == null) {
            replay = replayDao.findOneById(id);
            if (replay == null) {
                return ResponseResult.FAILED("评论不存在");
            }
            redisUtil.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE + replay.getArticleId());
            replay.setState(Constants.Comment.STATE_PUBLISH);
            replayDao.save(replay);
            return ResponseResult.SUCCESS("回复评论审核通过");
        }
        redisUtil.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE + firstcomment.getArticleId());
        firstcomment.setState(Constants.Comment.STATE_PUBLISH);
        firstCommentDao.save(firstcomment);
        return ResponseResult.SUCCESS("评论审核通过");
    }

    @Override
    public ResponseResult deleteCommentOrReplay(String id) {
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        Firstcomment firstcomment = firstCommentDao.findOneById(id);
        Replay replay;
        if (firstcomment == null) {
            replay = replayDao.findOneById(id);
            if (replay == null) {
                return ResponseResult.FAILED("评论不存在");
            }
            if (hewieUser.getId().equals(replay.getFromUid()) || Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
                //评论是当前用户的
                //或者是管理员账户
                replayDao.deleteById(id);
                return ResponseResult.SUCCESS("评论删除成功");
            } else {
                return ResponseResult.PERMISSION_DENIED();
            }
        }

        if (hewieUser.getId().equals(firstcomment.getUserId()) || Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
            //评论是当前用户的
            //或者是管理员账户
            firstCommentDao.deleteById(id);
            return ResponseResult.SUCCESS("评论删除成功");
        } else {
            return ResponseResult.PERMISSION_DENIED();
        }
    }

    @Override
    public ResponseResult listAllComments(int page, int size) {
        page = checkPage(page);
        size = checkSize(size);

        Sort sort = new Sort(Sort.Direction.DESC, "state", "createTime");
        Pageable pageable = PageRequest.of(page -1, size, sort);
        Page<Firstcomment> all = firstCommentDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取评论列表成功").setData(all);
    }

    @Override
    public ResponseResult listAllReplays(int page, int size) {
        page = checkPage(page);
        size = checkSize(size);

        Sort sort = new Sort(Sort.Direction.DESC, "state", "createTime");
        Pageable pageable = PageRequest.of(page -1, size, sort);
        Page<Replay> all = replayDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取评论列表成功").setData(all);
    }
}
