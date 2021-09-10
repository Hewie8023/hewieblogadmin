package com.hewie.blog.service;

import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.response.ResponseResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IUserService {

    ResponseResult initManagerAccount(HewieUser hewieUser, HttpServletRequest request);

    void createCaptcha(HttpServletResponse response) throws Exception;

    ResponseResult sendEmail(String type, HttpServletRequest request, String emailAddress, String captchaCode);

    ResponseResult register(HewieUser hewieUser, String emailCode, String captchaCode);

    ResponseResult doLogin(String captcha, HewieUser hewieUser, String from);

    HewieUser checkHewieUser();

    ResponseResult getUserInfo(String userId);

    ResponseResult checkEmail(String email);

    ResponseResult checkUserName(String userName);

    ResponseResult updateUserInfo(String userId, HewieUser hewieUser);

    ResponseResult deleteUserById(String userId);

    ResponseResult listUsers(int page, int size, String userName, String email);

    ResponseResult updateUserPassword(String verifyCode, HewieUser hewieUser);

    ResponseResult updateEmail(String email, String verifyCode);

    ResponseResult doLogout();

    ResponseResult getPcLoginQrCode();

    ResponseResult checkQrCodeLoginState(String loginId);

    ResponseResult updateQrCodeLoginState(String loginId);

    ResponseResult parseToken();


    ResponseResult resetPassword(String userId, String password);

    ResponseResult getRegisterCount();


    ResponseResult checkEmailCode(String email, String emailCode, String captchaCode);
}
