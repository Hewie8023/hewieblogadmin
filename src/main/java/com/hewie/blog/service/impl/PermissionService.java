package com.hewie.blog.service.impl;

import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.Constants;
import com.hewie.blog.utils.CookieUtils;
import com.hewie.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service("permission")
public class PermissionService {
    @Autowired
    private IUserService userService;
    /**
     * 判断是不是管理员
     * @return
     */
    public boolean admin() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String cookie = CookieUtils.getCookie(request, Constants.User.KEY_COOKIE_TOKEN);
        if (TextUtils.isEmpty(cookie)) {
            return false;
        }
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return false;
        }
        if (Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
            return true;
        }
        return false;
    }
}
