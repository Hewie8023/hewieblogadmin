package com.hewie.blog.service.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.hewie.blog.dao.ArticleNoContentDao;
import com.hewie.blog.dao.CommentDao;
import com.hewie.blog.pojo.ArticleNoContent;
import com.hewie.blog.pojo.Comment;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.pojo.PageList;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ICommentService;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.Date;

@Slf4j
@Service
@Transactional
public class CommentServiceImpl extends BaseService implements ICommentService {
    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private IUserService userService;

    @Autowired
    private ArticleNoContentDao articleNoContentDao;

    @Override
    public ResponseResult postComment(Comment comment) {

        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        //检查内容
        String articleId = comment.getArticleId();
        if (TextUtils.isEmpty(articleId)) {
            return ResponseResult.FAILED("文章ID不能为空");
        }
        ArticleNoContent articleFromDb = articleNoContentDao.findOneById(articleId);
        if (articleFromDb == null) {
            return ResponseResult.FAILED("文章不存在");
        }
        String content = comment.getContent();
        if (TextUtils.isEmpty(content)) {
            return ResponseResult.FAILED("评论内容不能为空");
        }

        //补全内容
        comment.setId(idWorker.nextId() + "");
        comment.setUserAvatar(hewieUser.getAvatar());
        comment.setUserName(hewieUser.getUserName());
        comment.setUserId(hewieUser.getId());

        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());

        //保存入库
        commentDao.save(comment);

        //清除文章的评论缓存
        redisUtil.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE + comment.getArticleId());

        //todo：发送通知：邮件
        //EmailSender.sendCommentNotify("你的文字被评论了...");

        //返回结果
        return ResponseResult.SUCCESS("添加评论成功");
    }

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private Gson gson;

    /**
     * 获取文章评论列表
     * 排序策略：
     * 基本的按照时间顺序
     *
     * 置顶的一定在最前面
     *
     * 后发表的：前n个小时排在最前面，之后按照点赞量排序
     * @param articleId
     * @param page
     * @param size
     * @return
     */
    @Override
    public ResponseResult listCommentsByArticleId(String articleId, int page, int size) {
        page = checkPage(page);
        size = checkSize(size);

        if (page == 1) {

            //如果是第一页，先从缓存中拿
            //如果没有，往下走
            String commentCacheJson = (String) redisUtil.get(Constants.Comment.KEY_COMMENT_FIRST_PAGE + articleId);
            if (!TextUtils.isEmpty(commentCacheJson)) {
                PageList<Comment> result = gson.fromJson(commentCacheJson, new TypeToken<PageList<Comment>>(){}.getType());
                return ResponseResult.SUCCESS("获取文章评论列表成功").setData(result);
            }

        }
        Sort sort = new Sort(Sort.Direction.DESC, "state","createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Comment> all = commentDao.findAllByArticleId(articleId, pageable);
        PageList<Comment> result = new PageList<>();
        result.parsePage(all);
        //保存一份到缓存
        if (page == 1) {
            redisUtil.set(Constants.Comment.KEY_COMMENT_FIRST_PAGE + articleId, gson.toJson(result), Constants.TimeValueInSecond.HOUR);
        }
        return ResponseResult.SUCCESS("获取文章评论列表成功").setData(result);
    }

    @Override
    public ResponseResult deleteComment(String commentId) {
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        Comment commentFromDb = commentDao.findOneById(commentId);
        if (commentFromDb == null) {
            return ResponseResult.FAILED("评论不存在");
        }

        if (hewieUser.getId().equals(commentFromDb.getUserId()) || Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
            //评论是当前用户的
            //或者是管理员账户
            commentDao.deleteById(commentId);
            return ResponseResult.SUCCESS("评论删除成功");
        } else {
            return ResponseResult.PERMISSION_DENIED();
        }

    }

    @Override
    public ResponseResult listComments(int page, int size) {
        page = checkPage(page);
        size = checkSize(size);

        Sort sort = new Sort(Sort.Direction.DESC, "state", "createTime");
        Pageable pageable = PageRequest.of(page -1, size, sort);
        Page<Comment> all = commentDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取评论列表成功").setData(all);
    }

    @Override
    public ResponseResult topComment(String commentId) {
        Comment comment = commentDao.findOneById(commentId);
        if (comment == null) {
            return ResponseResult.FAILED("评论不存在");
        }
        redisUtil.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE + comment.getArticleId());
        String state = comment.getState();
        if (Constants.Comment.STATE_TOP.equals(state)) {
            comment.setState(Constants.Comment.STATE_PUBLISH);
            comment.setUpdateTime(new Date());
            commentDao.save(comment);
            return ResponseResult.SUCCESS("取消置顶成功");
        } else if (Constants.Comment.STATE_PUBLISH.equals(state)) {
            comment.setState(Constants.Comment.STATE_TOP);
            comment.setUpdateTime(new Date());
            commentDao.save(comment);
            return ResponseResult.SUCCESS("置顶成功");
        } else {
            return ResponseResult.FAILED("评论状态非法");
        }
    }

    @Override
    public ResponseResult getCommentCount() {
        long count = commentDao.count();
        return ResponseResult.SUCCESS("获取评论总量成功").setData(count);
    }
}
