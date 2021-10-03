package com.hewie.blog.interceptor;

import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.google.gson.Gson;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.utils.Constants;
import com.hewie.blog.utils.CookieUtils;
import com.hewie.blog.utils.RedisUtil;
import com.hewie.blog.utils.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.lang.reflect.Method;

@Component
@Slf4j
public class ApiInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private Gson gson;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            CheckTooFrequentCommit methodAnnotation = handlerMethod.getMethodAnnotation(CheckTooFrequentCommit.class);
            if (methodAnnotation != null) {
                String methodName = handlerMethod.getMethod().getName();
                //所有提交内容的方法，必须用户登录，使用token作为key来记录请求频率
                String tokenKey = CookieUtils.getCookie(request, Constants.User.KEY_COOKIE_TOKEN);
                log.info("tokenKey == >" + tokenKey);
                if (!TextUtils.isEmpty(tokenKey)) {
                    //从redis获取，若存在返回提交太频繁
                    String hasCommit = (String) redisUtil.get(Constants.User.KEY_COMMIT_TOKEN_RECORD + tokenKey + methodName);
                    //判断是否真的提交太频繁
                    if (!TextUtils.isEmpty(hasCommit)) {
                        log.info("check commit too frequent...");
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType("application/json");
                        ResponseResult failed = ResponseResult.FAILED("提交过于频繁，请稍候重试");
                        PrintWriter writer = response.getWriter();
                        writer.write(gson.toJson(failed));
                        writer.flush();
                        return false;
                    } else {
                        redisUtil.set(Constants.User.KEY_COMMIT_TOKEN_RECORD + tokenKey + methodName, "true", Constants.TimeValueInSecond.FIVE_SEC);
                    }
                }
            }
        }
        //true表示放行,false表示拦截
        return true;
    }
}
