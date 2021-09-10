package com.hewie.blog.controller;

import com.hewie.blog.dao.CommentDao;
import com.hewie.blog.dao.LabelDao;
import com.hewie.blog.pojo.Comment;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.pojo.Label;
import com.hewie.blog.pojo.User;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.response.ResponseState;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.service.impl.SolrTestService;
import com.hewie.blog.utils.*;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Transactional
@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private LabelDao labelDao;

    @Autowired
    private SnowflakeIdWorker idWorker;

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public ResponseResult helloworld() {
        log.info("hello world ...");
        String captchaContent = (String)redisUtil.get(Constants.User.KEY_CAPTCHA_CONTENT + "123456");
        log.info("captchaContent == >" + captchaContent);
        return ResponseResult.SUCCESS().setData("hello hewie");
    }

    @GetMapping("/json")
    public ResponseResult testJson() {
        User user = new User("hewie", 26, "male");
        return ResponseResult.FAILED().setData(user);
    }

    @PostMapping("/login")
    public ResponseResult testLogin(@RequestBody User user) {
        System.out.println("userName : " + user.getUserName());
        ResponseResult responseResult = new ResponseResult(ResponseState.LOGIN_SUCCESS);
        return responseResult.setData(user);
    }

    @PostMapping("/label")
    public ResponseResult addLabel(@RequestBody Label label) {
        label.setId(idWorker.nextId() + "");
        label.setCreateTime(new Date());
        label.setUpdateTime(new Date());
        labelDao.save(label);
        return ResponseResult.SUCCESS("添加label成功");
    }

    @DeleteMapping("/label/{labelId}")
    public ResponseResult deleteLabel(@PathVariable("labelId") String labelId) {
        try {
            labelDao.deleteById(labelId);
        } catch (Exception e) {
            return ResponseResult.FAILED("删除失败").setData(labelId);
        }
        return ResponseResult.SUCCESS("删除label成功").setData(labelId);
    }

    @PutMapping("/label/{labelId}")
    public ResponseResult updateLabel(@PathVariable("labelId") String labelId,
                                      @RequestBody Label label) {
        Label dbLabel = labelDao.findOneById(labelId);
        if (dbLabel == null) {
            return ResponseResult.FAILED("label不存在").setData(labelId);
        }
        dbLabel.setCount(label.getCount());
        dbLabel.setName(label.getName());
        dbLabel.setUpdateTime(new Date());
        labelDao.save(dbLabel);

        return ResponseResult.SUCCESS("修改标签成功").setData(dbLabel);
    }

    @GetMapping("/label/{labelId}")
    public ResponseResult getLabelById(@PathVariable("labelId") String labelId) {
        Label dbLabel = labelDao.findOneById(labelId);
        if (dbLabel == null) {
            return ResponseResult.FAILED("label不存在").setData(labelId);
        }
        return ResponseResult.SUCCESS("查询成功").setData(dbLabel);
    }

    @GetMapping("/label/list/{page}/{size}")
    public ResponseResult listLabels(@PathVariable("page") int page, @PathVariable("size") int size) {
        if (page < 1) {
            page = 1;
        }
        if (size <= 0) {
            size = Constants.Page.DEFAULT_PAGE;
        }
        Sort sort = new Sort(Sort.Direction.ASC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Label> result = labelDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取label列表成功").setData(result);
    }

    @GetMapping("/label/search")
    public ResponseResult doLabelSearch(@RequestParam("keyword") String keyword, @RequestParam("count") int count) {
        List<Label> result = labelDao.findAll(new Specification<Label>() {
            @Override
            public Predicate toPredicate(Root<Label> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                Predicate namePre = cb.like(root.get("name").as(String.class), "%" + keyword + "%");
                Predicate countPre = cb.equal(root.get("count").as(Integer.class), count);
                Predicate and = cb.and(namePre, countPre);
                return and;
            }
        });

        if (result == null || result.size() == 0) {
            return ResponseResult.FAILED("结果为空");
        }
        return ResponseResult.SUCCESS("查找成功").setData(result);
    }

    @Autowired
    private RedisUtil redisUtil;

    //http://localhost:80/test/captcha
    @RequestMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 设置请求头为输出图片类型
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // 三个参数分别为宽、高、位数
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        // 设置字体
        // specCaptcha.setFont(new Font("Verdana", Font.PLAIN, 32));  // 有默认字体，可以不用设置
        specCaptcha.setFont(Captcha.FONT_1);
        // 设置类型，纯数字、纯字母、字母数字混合
        //specCaptcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        specCaptcha.setCharType(Captcha.TYPE_DEFAULT);

        String content = specCaptcha.text().toLowerCase();
        log.info("captcha content == > " + content);
        // 验证码存入session
        //request.getSession().setAttribute("captcha", content);
        //保存到redis，10分钟有效
        redisUtil.set(Constants.User.KEY_CAPTCHA_CONTENT + "123456", content, 60 * 10);

        // 输出图片流
        specCaptcha.out(response.getOutputStream());
    }

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private IUserService userService;

    @PostMapping("/comment")
    public ResponseResult testComment(@RequestBody Comment comment, HttpServletRequest request, HttpServletResponse response) {
        String content = comment.getContent();
        log.info("comment content == >" + content);
        String tokenKey = CookieUtils.getCookie(request, Constants.User.KEY_COOKIE_TOKEN);
        if (tokenKey == null) {
            return ResponseResult.FAILED("账户未登录");
        }

        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.FAILED("账户未登录");
        }

        comment.setUserId(hewieUser.getId());
        comment.setUserAvatar(hewieUser.getAvatar());
        comment.setUserName(hewieUser.getUserName());
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());
        comment.setId(idWorker.nextId() + "");

        commentDao.save(comment);

        return ResponseResult.SUCCESS("评论成功").setData(comment);
    }

    @Autowired
    private SolrTestService solrTestService;

    @PostMapping("/solr")
    public ResponseResult solrAdd() {
        solrTestService.add();
        return ResponseResult.SUCCESS("success");
    }

    @PutMapping("/solr")
    public ResponseResult solrUpdate() {
        solrTestService.update();
        return ResponseResult.SUCCESS("update success");
    }

    @DeleteMapping("/solr")
    public ResponseResult solrDelete() {
        solrTestService.delete();
        return ResponseResult.SUCCESS("delete success");
    }

    @PostMapping("/solr/all")
    public ResponseResult solrAddAll() {
        solrTestService.importAll();
        return ResponseResult.SUCCESS("success");
    }

    @DeleteMapping("/solr/all")
    public ResponseResult solrDeleteAll() {
        solrTestService.deleteAll();
        return ResponseResult.SUCCESS("delete success");
    }
}
