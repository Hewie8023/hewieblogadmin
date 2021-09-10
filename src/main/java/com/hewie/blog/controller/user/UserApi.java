package com.hewie.blog.controller.user;

import com.hewie.blog.interceptor.CheckTooFrequentCommit;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.SnowflakeIdWorker;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
@RestController
@RequestMapping(value = "/user")
public class UserApi {

    @Autowired
    private IUserService userService;

    @Autowired
    private SnowflakeIdWorker idWorker;


    /**
     * 初始化管理员账户
     *
     * @param hewieUser
     * @return
     */
    @PostMapping("/admin_account")
    public ResponseResult initManager(@RequestBody HewieUser hewieUser, HttpServletRequest request) {
        log.info("userName ==> " + hewieUser.getUserName());
        log.info("password ==> " + hewieUser.getPassword());
        return userService.initManagerAccount(hewieUser, request);
    }

    /**
     * 注册
     * @param hewieUser
     * @return
     */
    @PostMapping("/join_in")
    public ResponseResult register(@RequestBody HewieUser hewieUser,
                                   @RequestParam("email_code") String emailCode,
                                   @RequestParam("captcha_code") String captchaCode) {

        return userService.register(hewieUser, emailCode, captchaCode);
    }

    /**
     * 登录
     *
     * 需要提交的数据
     * 1、账户（用户名/邮箱：唯一处理）
     * 2、密码
     * 3、图灵验证码
     * 4、图灵验证码的key
     *
     * @param captcha
     * @param hewieUser
     * @return
     */
    @PostMapping("/login/{captcha}")
    public ResponseResult login(@PathVariable("captcha") String captcha,
                                @RequestBody HewieUser hewieUser,
                                @RequestParam(value = "from", required = false) String from) {
        return userService.doLogin(captcha, hewieUser, from);
    }

    /**
     * 获取验证码
     * @return
     */
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response) {
        try {
            userService.createCaptcha(response);
        } catch (Exception e) {
            log.info(e.toString());
        }


    }

    /**
     * 发送邮件email
     *
     * 使用场景：注册、找回密码、修改邮箱（会输入新的邮箱）
     * 注册：如果已经注册，提示该邮箱已经注册
     * 找回密码：如果没有注册，提示该邮箱未注册
     * 修改邮箱：新的邮箱：如果已经注册，提示该邮箱已经注册。
     *
     * @return
     */
    @GetMapping("/verify_code")
    public ResponseResult sendVerifyCode(HttpServletRequest request,
                                         @RequestParam("type") String type,
                                         @RequestParam("email") String emailAddress,
                                         @RequestParam("captchaCode") String captchaCode) {
        log.info("email ==> " + emailAddress);
        return userService.sendEmail(type, request, emailAddress, captchaCode);
    }

    /**
     * 修改用户密码
     *
     * 普通做法：通过旧密码对比来更新密码
     *
     * 找回密码、修改密码
     * 发送验证码到邮箱/手机，判断验证码是否正确
     *
     * 步骤：
     * 1、用户填写邮箱
     * 2、用户获取验证码 type：forget
     * 3、用户填写验证码、新的密码
     * 4、提交数据
     *
     * 数据包括：
     * 1、邮箱和新密码
     * 2、验证码
     *
     * 如果验证码正确，所用邮箱注册的账户就是你的，可以修改
     *
     * @param verifyCode
     * @param hewieUser
     * @return
     */
    @PutMapping("/password/{verify_code}")
    public ResponseResult updatePassword(@PathVariable("verify_code") String verifyCode, @RequestBody HewieUser hewieUser) {
        return userService.updateUserPassword(verifyCode, hewieUser);
    }


    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @GetMapping("/user_info/{userId}")
    public ResponseResult getUserInfo(@PathVariable("userId") String userId) {

        return userService.getUserInfo(userId);
    }

    /**
     * 修改用户信息
     *
     * 允许用户修改的内容
     * 1、头像
     * 2、用户名（唯一）
     * 3、密码（单独接口修改）
     * 4、签名
     * 5、email（唯一）（单独接口修改）
     *
     * @param userId
     * @param hewieUser
     * @return
     */
    @PutMapping("/user_info/{userId}")
    public ResponseResult updateUserInfo(@PathVariable("userId") String userId,
                                         @RequestBody HewieUser hewieUser) {
        return userService.updateUserInfo(userId, hewieUser);
    }

    /**
     * 获取用户列表
     * 需要管理员权限
     * @param page
     * @param size
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @GetMapping("/list")
    public ResponseResult listUser(@RequestParam("page") int page,
                                   @RequestParam("size") int size,
                                   @RequestParam(value = "userName", required = false) String userName,
                                   @RequestParam(value = "email", required = false) String email) {

        return userService.listUsers(page, size, userName, email);
    }

    /**
     * 删除用户
     *
     * 需要管理员权限
     *
     * @param userId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{userId}")
    public ResponseResult deleteUser(@PathVariable("userId") String userId) {

        return userService.deleteUserById(userId);
    }

    /**
     * 检查该email是否已经注册
     * @param email
     * @return success：已经注册。fail：未注册
     */
    @ApiResponses({
            @ApiResponse(code = 2000, message = "表示当前邮箱已经注册"),
            @ApiResponse(code = 4000, message = "表示当前邮箱未注册")
    })
    @GetMapping("/email")
    public ResponseResult checkEmail(@RequestParam("email") String email){
        return userService.checkEmail(email);
    }

    /**
     * 检查该用户名是否已经注册
     * @param userName
     * @return success：已经注册。fail：未注册
     */
    @ApiResponses({
            @ApiResponse(code = 2000, message = "表示当前用户名已经注册"),
            @ApiResponse(code = 4000, message = "表示当前用户名未注册")
    })
    @GetMapping("/user_name")
    public ResponseResult checkUserName(@RequestParam("user_name") String userName){
        return userService.checkUserName(userName);
    }

    /**
     * 更新邮箱
     *
     * 1、必须登录
     * 2、新的邮箱没有注册
     *
     * 步骤
     * 1、已经登录
     * 2、输入新的邮箱
     * 3、获取验证码type=update
     * 4、输入验证码
     * 5、提交数据
     *
     * 数据：新的邮箱地址、验证码其他信息从token获取
     * @return
     */
    @PutMapping("/email")
    public ResponseResult updateEmail(@RequestParam("email") String email, @RequestParam("verify_code") String verifyCode) {
        return userService.updateEmail(email, verifyCode);
    }

    /**
     * 退出登录
     *
     * 拿到tokenKey
     * 删除redis对应的token
     * 删除mysql里对应的refreshToken
     * 删除cookie里的tokenKey
     *
     * @return
     */
    @GetMapping("/logout")
    public ResponseResult logout() {
        return userService.doLogout();
    }


    /**
     * 获取二维码
     * 二维码的图片路径
     * 二维码的内容字符串
     * @return
     */
    @GetMapping("/pc-login-qr-code")
    public ResponseResult getPcLoginQrCode(){
        //生成一个唯一的id
        //保存到redis，值为false，时间为5分钟（二维码有效期）
        //返回结果
        return userService.getPcLoginQrCode();
    }

    /**
     * 检查二维码登录状态
     * @return
     */
    @GetMapping("/qr-code-state/{loginId}")
    public ResponseResult checkQrCodeLoginState(@PathVariable("loginId") String loginId) {
        return userService.checkQrCodeLoginState(loginId);
    }

    @PutMapping("/qr-code-state/{loginId}")
    public ResponseResult updateQrCodeLoginState(@PathVariable("loginId") String loginId) {
        return userService.updateQrCodeLoginState(loginId);
    }

    @GetMapping("/check-token")
    public ResponseResult parseToken() {
        return userService.parseToken();
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/reset-password/{userId}")
    public ResponseResult resetPassword(@PathVariable("userId") String userId, @RequestParam("password") String password) {
        return userService.resetPassword(userId, password);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/register_count")
    public ResponseResult getRegisterCount() {
        return userService.getRegisterCount();
    }

    @GetMapping("/check-email-code")
    public ResponseResult checkEmailCode(@RequestParam("email") String email,
                                         @RequestParam("emailCode") String emailCode,
                                         @RequestParam("captchaCode") String captchaCode){
        return userService.checkEmailCode(email, emailCode, captchaCode);
    }
}
