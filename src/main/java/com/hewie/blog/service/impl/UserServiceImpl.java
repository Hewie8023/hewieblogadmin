package com.hewie.blog.service.impl;

import com.google.gson.Gson;
import com.hewie.blog.dao.RefreshTokenDao;
import com.hewie.blog.dao.SettingsDao;
import com.hewie.blog.dao.UserDao;
import com.hewie.blog.dao.UserNoPasswordDao;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.pojo.HewieUserNoPassword;
import com.hewie.blog.pojo.RefreshToken;
import com.hewie.blog.pojo.Setting;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.response.ResponseState;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.*;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
class UserServiceImpl extends BaseService implements IUserService {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private UserDao userDao;

    @Autowired
    private SettingsDao settingsDao;

    @Autowired
    private Random random;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RefreshTokenDao refreshTokenDao;

    @Autowired
    private Gson gson;

    @Override
    public ResponseResult initManagerAccount(HewieUser hewieUser, HttpServletRequest request) {
        // 先检查是否有初始化
        Setting managerAccountState = settingsDao.findOneByKey(Constants.Settings.MANAGER_ACCOUNT_INIT);
        if (managerAccountState != null) {
            return ResponseResult.FAILED("管理员账户已经初始化！！！");
        }
        // 检查数据
        if (TextUtils.isEmpty(hewieUser.getUserName())) {
            return ResponseResult.FAILED("用户名不能为空");
        }
        if (TextUtils.isEmpty(hewieUser.getPassword())) {
            return ResponseResult.FAILED("密码不能为空");
        }
        if (TextUtils.isEmpty(hewieUser.getEmail())) {
            return ResponseResult.FAILED("邮箱不能为空");
        }
        // 补充数据
        hewieUser.setId(String.valueOf(idWorker.nextId()));
        hewieUser.setRoles(Constants.User.ROLE_ADMIN);
        hewieUser.setAvatar(Constants.User.DEFAULT_AVATAR);
        hewieUser.setState(Constants.User.DEFAULT_STATE);
        hewieUser.setRegIp(request.getRemoteAddr());
        hewieUser.setLoginIp(request.getRemoteAddr());
        hewieUser.setCreateTime(new Date());
        hewieUser.setUpdateTime(new Date());
        //对密码进行加密
        String originalPassword = hewieUser.getPassword();
        String encodePassword = bCryptPasswordEncoder.encode(originalPassword);
        hewieUser.setPassword(encodePassword);

        // 保存到数据库
        userDao.save(hewieUser);
        // 更新已经添加的标记
        Setting setting = new Setting();
        setting.setId(String.valueOf(idWorker.nextId()));
        setting.setKey(Constants.Settings.MANAGER_ACCOUNT_INIT);
        setting.setCreateTime(new Date());
        setting.setUpdateTime(new Date());
        setting.setValue("1");
        settingsDao.save(setting);
        return ResponseResult.SUCCESS("初始化成功");
    }

    public static final int[] captcha_font_types = {
            Captcha.FONT_1,
            Captcha.FONT_2,
            Captcha.FONT_3,
            Captcha.FONT_4,
            Captcha.FONT_5,
            Captcha.FONT_6,
            Captcha.FONT_7,
            Captcha.FONT_8,
            Captcha.FONT_9,
            Captcha.FONT_10
    };

    /**
     * 获取图灵验证码
     * @param response
     * @throws Exception
     */
    @Override
    public void createCaptcha(HttpServletResponse response) throws Exception {
        String lastId = CookieUtils.getCookie(getRequest(), Constants.User.KEY_LAST_CAPTCHA_ID);
        String key;
        if (TextUtils.isEmpty(lastId)) {
            key = idWorker.nextId() + "";
        } else {
            key = lastId;
        }

        // 处理
        // 设置请求头为输出图片类型
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        int captchaType = random.nextInt(3);
        Captcha targetCaptcha = null;
        int width = 120;
        int height = 40;
        if (captchaType == 0) {
            targetCaptcha = new SpecCaptcha(width, height, 5);
        } else if (captchaType == 1) {
            // gif类型
            targetCaptcha = new GifCaptcha(width, height);
        } else{
            // 算术类型
            targetCaptcha = new ArithmeticCaptcha(width, height);
            targetCaptcha.setLen(2);  // 几位数运算，默认是两位
        }
        // 设置字体
        // specCaptcha.setFont(new Font("Verdana", Font.PLAIN, 32));  // 有默认字体，可以不用设置
        targetCaptcha.setFont(captcha_font_types[random.nextInt(captcha_font_types.length)]);
        // 设置类型，纯数字、纯字母、字母数字混合
        //specCaptcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        targetCaptcha.setCharType(Captcha.TYPE_DEFAULT);
        String content = targetCaptcha.text().toLowerCase();
        log.info("captcha content == > " + content);
        //保存到redis，10分钟有效
        //删除时机：1、时间过期 2、验证码用完后删除
        //用完的情况：在get的地方
        CookieUtils.setUpCookie(response, Constants.User.KEY_LAST_CAPTCHA_ID, key);
        redisUtil.set(Constants.User.KEY_CAPTCHA_CONTENT + key, content, 60 * 2);

        targetCaptcha.out(response.getOutputStream());
    }

    @Autowired
    private TaskService taskService;
    /**
     * 发送邮件:验证码
     * 使用场景：注册、找回密码、修改邮箱（会输入新的邮箱）
     * 注册(register)：如果已经注册，提示该邮箱已经注册
     * 找回密码(forget)：如果没有注册，提示该邮箱未注册
     * 修改邮箱(update)：新的邮箱：如果已经注册，提示该邮箱已经注册。
     * @param request
     * @param emailAddress
     * @return
     */
    @Override
    public ResponseResult sendEmail(String type, HttpServletRequest request, String emailAddress, String captchaCode) {
        //检查人类验证码是否正确
        String captchaId = CookieUtils.getCookie(request, Constants.User.KEY_LAST_CAPTCHA_ID);
        String captchaVal = (String) redisUtil.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaId);
        if (!captchaCode.equals(captchaVal)) {
            return ResponseResult.FAILED("人类验证码不正确");
        }

        if (emailAddress == null) {
            return ResponseResult.FAILED("邮箱地址不能为空");
        }
        // 根据类型，查询邮箱是否注册
        if ("register".equals(type) || "update".equals(type)) {
            HewieUser userByEmail = userDao.findOneByEmail(emailAddress);
            if (userByEmail != null) {
                return ResponseResult.FAILED("该邮箱已经注册");
            }
        } else if ("forget".equals(type)) {
            HewieUser userByEmail = userDao.findOneByEmail(emailAddress);
            if (userByEmail == null) {
                return ResponseResult.FAILED("该邮箱未注册");
            }
        }

        // 1、防止暴力发送：不间断:同一个邮箱，间隔要超过30s，同一个ip最多能发10次（如果是短信，最多发3次）
        String remoteAddr = request.getRemoteAddr();

        if (remoteAddr != null) {
            remoteAddr = remoteAddr.replace(":", "_");
        }
        log.info("remodteAddr == >" + remoteAddr);
        // 取出，如果没有：过，若有：判断次数
        String ipSendTimeStr = (String) redisUtil.get(Constants.User.KEY_EMAIL_SEND_IP + remoteAddr);
        Integer ipSendTimes;
        if (ipSendTimeStr != null) {
            ipSendTimes = Integer.parseInt(ipSendTimeStr);
        } else {
            ipSendTimes = 1;
        }
        if (ipSendTimes > 10) {
            return ResponseResult.FAILED("请不要频繁发送验证码");
        }
        String emailAddressSend = (String) redisUtil.get(Constants.User.KEY_EMAIL_SEND_ADDRESS + emailAddress);
        if (emailAddressSend != null) {
            return ResponseResult.FAILED("请不要频繁获取验证码");
        }
        // 2、检查邮箱地址是否正确
        if (!TextUtils.isEmailAddress(emailAddress)) {
            return ResponseResult.FAILED("邮箱地址格式不正确");
        }
        // 3、发送验证码:6位数（100000 - 999999）
        int code = random.nextInt(999999);
        if (code < 100000) {
            code += 100000;
        }
        log.info("sendEmail  code ==>" + code);
        try {
            taskService.sendEmailVerifyCode(String.valueOf(code), emailAddress);
        } catch (Exception e) {
            return ResponseResult.FAILED("验证码发送失败，请稍后重试！");
        }
        // 4、 记录
        if (ipSendTimes == null) {
            ipSendTimes = 0;
        }
        ipSendTimes++;
        redisUtil.set(Constants.User.KEY_EMAIL_SEND_IP + remoteAddr, String.valueOf(ipSendTimes), 60 * 60);
        redisUtil.set(Constants.User.KEY_EMAIL_SEND_ADDRESS + emailAddress, "send", 60);
        //保存code
        redisUtil.set(Constants.User.KEY_EMAIL_CONTENT + emailAddress, String.valueOf(code), 60 * 2);
        return ResponseResult.SUCCESS("验证码发送成功").setData(code);
    }

    /**
     * 用户注册
     * @param hewieUser
     * @return
     */
    @Override
    public ResponseResult register(HewieUser hewieUser, String emailCode, String captchaCode) {
        // 1、检查当前用户是否已经注册
        String userName = hewieUser.getUserName();
        if (TextUtils.isEmpty(userName)) {
            return ResponseResult.FAILED("用户名不能为空");
        }
        HewieUser userFromDbByUserName = userDao.findOneByUserName(userName);
        if (userFromDbByUserName != null) {
            return ResponseResult.FAILED("该用户已经注册");
        }

        // 2、检查邮箱格式是否正确
        String email = hewieUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            return ResponseResult.FAILED("邮箱地址不能为空");
        }
        if (!TextUtils.isEmailAddress(email)) {
            return ResponseResult.FAILED("邮箱格式不正确");
        }

        // 3、检查邮箱是否已经注册
        HewieUser userFromDbByEmail = userDao.findOneByEmail(email);
        if (userFromDbByEmail != null) {
            return ResponseResult.FAILED("该邮箱已经注册");
        }

        // 4、检查邮箱验证码是否正确
        String emailVerifyCode = (String) redisUtil.get(Constants.User.KEY_EMAIL_CONTENT + email);
        if (TextUtils.isEmpty(emailVerifyCode)) {
            return ResponseResult.FAILED("验证码无效，请重新获取");
        }
        if (!emailVerifyCode.equals(emailCode)) {
            return ResponseResult.FAILED("邮箱验证码不正确");
        } else {
            //正确，干掉redis的记录
            redisUtil.del(Constants.User.KEY_EMAIL_CONTENT + email);
        }
        HttpServletRequest request = getRequest();
        String captchaKey = CookieUtils.getCookie(request, Constants.User.KEY_LAST_CAPTCHA_ID);
        if (TextUtils.isEmpty(captchaKey)) {
            return ResponseResult.FAILED("请允许保留cookie信息");
        }
        // 5、检查图灵验证码
        String captchaCodeFromRedis = (String) redisUtil.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (TextUtils.isEmpty(captchaCodeFromRedis)) {
            return ResponseResult.FAILED("图灵验证码已过期");
        }
        if (!captchaCodeFromRedis.equals(captchaCode)) {
            return ResponseResult.FAILED("图灵验证码不正确");
        } else {
            redisUtil.del(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        }

        // 可以注册
        // 6、 对密码进行加密
        String password = hewieUser.getPassword();
        if (TextUtils.isEmpty(password)) {
            return ResponseResult.FAILED("密码不能为空");
        }
        hewieUser.setPassword(bCryptPasswordEncoder.encode(password));

        // 7、补全数据：ip、时间、id、角色、头像

        hewieUser.setId(idWorker.nextId() + "");
        hewieUser.setRegIp(request.getRemoteAddr());
        hewieUser.setLoginIp(request.getRemoteAddr());
        hewieUser.setAvatar(Constants.User.DEFAULT_AVATAR);
        hewieUser.setCreateTime(new Date());
        hewieUser.setUpdateTime(new Date());
        hewieUser.setRoles(Constants.User.ROLE_NORMAL);
        hewieUser.setState("1");

        // 8、保存数据
        userDao.save(hewieUser);
        CookieUtils.deleteCookie(getResponse(),  Constants.User.KEY_LAST_CAPTCHA_ID);
        // 9、返回结果
        return ResponseResult.GET(ResponseState.JOIN_SUCCESS);
    }

    /**
     * 登录
     * @param captcha
     * @param hewieUser
     * @param from
     * @return
     */
    @Override
    public ResponseResult doLogin(String captcha,
                                  HewieUser hewieUser,
                                  String from) {
        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();
        if (TextUtils.isEmpty(from) || (!Constants.FROM_MOBILE.equals(from) && !Constants.FROM_PC.equals(from))) {
            from = Constants.FROM_MOBILE;
        }
        String captchaKey = CookieUtils.getCookie(request, Constants.User.KEY_LAST_CAPTCHA_ID);
        if (TextUtils.isEmpty(captchaKey)) {
            return ResponseResult.FAILED("请允许保留cookie信息");
        }
        // 验证图灵验证码
        String captchaValue = (String) redisUtil.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (TextUtils.isEmpty(captchaValue)) {
            return ResponseResult.FAILED("图灵验证码已过期");
        }
        redisUtil.del(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (!captchaValue.equals(captcha)) {
            return ResponseResult.FAILED("图灵验证码不正确");
        }

        String userName = hewieUser.getUserName();
        if (TextUtils.isEmpty(userName)) {
            return ResponseResult.FAILED("账户不能为空");
        }

        String password = hewieUser.getPassword();
        if (TextUtils.isEmpty(password)) {
            return ResponseResult.FAILED("密码不能为空");
        }

        HewieUser userFromDb = userDao.findOneByUserName(userName);
        if (userFromDb == null) {
            userFromDb = userDao.findOneByEmail(hewieUser.getEmail());
        }
        if (userFromDb == null) {
            return ResponseResult.FAILED("用户名或者密码错误");
        }
        if (!bCryptPasswordEncoder.matches(password, userFromDb.getPassword())) {
            return ResponseResult.FAILED("用户名或者密码错误");
        }

        //判断用户状态
        String state = userFromDb.getState();
        if (!"1".equals(state)) {
            return ResponseResult.ACCOUNT_DENIED();
        }
        //修改更新时间和登录ip
        userFromDb.setLoginIp(request.getRemoteAddr());
        userFromDb.setUpdateTime(new Date());

        createToken(response, userFromDb, from);
        CookieUtils.deleteCookie(response,  Constants.User.KEY_LAST_CAPTCHA_ID);
        return ResponseResult.GET(ResponseState.LOGIN_SUCCESS);
    }

    /**
     * @param response
     * @param userFromDb
     * @param from
     * @return tokenKey
     */
    private String createToken(HttpServletResponse response, HewieUser userFromDb, String from) {
        String oldTokenKey = CookieUtils.getCookie(getRequest(), Constants.User.KEY_COOKIE_TOKEN);
        RefreshToken oldRefreshToken = refreshTokenDao.findOneByUserId(userFromDb.getId());
        //删掉refreshToken的记录
        //refreshTokenDao.deleteAllByUserId(userFromDb.getId());
        if (Constants.FROM_MOBILE.equals(from)) {
            if (oldRefreshToken != null) {
                redisUtil.del(Constants.User.KEY_TOKEN + oldRefreshToken.getMobileTokenKey());
            }
            refreshTokenDao.deleteMobileTokenKey(oldTokenKey);
        } else if (Constants.FROM_PC.equals(from)) {
            if (oldRefreshToken != null) {
                redisUtil.del(Constants.User.KEY_TOKEN + oldRefreshToken.getTokenKey());
            }
            refreshTokenDao.deletePcTokenKey(oldTokenKey);
        }

        //生成token
        Map<String, Object> claims = ClaimsUtils.user2Claims(userFromDb, from);

        String token = JwtUtil.createToken(claims);
        //返回token的md5值，token保存在redis
        //前端访问时携带token的md5值，从redis里获取信息
        String tokenKey = from + DigestUtils.md5DigestAsHex(token.getBytes());
        //token保存到redis，2个小时有效期
        redisUtil.set(Constants.User.KEY_TOKEN + tokenKey, token, Constants.TimeValueInSecond.HOUR_2);

        // key写到cookies里
        CookieUtils.setUpCookie(response, Constants.User.KEY_COOKIE_TOKEN, tokenKey);

        //先判斷數據庫有没有，如果有的话更新，没有就新创建
        RefreshToken refreshToken = refreshTokenDao.findOneByUserId(userFromDb.getId());
        if (refreshToken == null) {
            refreshToken = new RefreshToken();
            refreshToken.setId(idWorker.nextId() + "");
            refreshToken.setCreateTime(new Date());
            refreshToken.setUserId(userFromDb.getId());
        }
        // 生成refreshToken
        String refreshTokenValue = JwtUtil.createRefreshToken(userFromDb.getId(), Constants.TimeValueInMillions.MONTH);
        //保存数据库

        refreshToken.setRefreshToken(refreshTokenValue);
        //判断来源
        if (Constants.FROM_MOBILE.equals(from)) {
            refreshToken.setMobileTokenKey(tokenKey);
        } else if (Constants.FROM_PC.equals(from)) {
            refreshToken.setTokenKey(tokenKey);
        }
        refreshToken.setUpdateTime(new Date());
        refreshTokenDao.save(refreshToken);

        return tokenKey;
    }

    /**
     * 校验用户是否登录
     * @return
     */
    @Override
    public HewieUser checkHewieUser() {
        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();
        String tokenKey = CookieUtils.getCookie(request, Constants.User.KEY_COOKIE_TOKEN);
        if (TextUtils.isEmpty(tokenKey)) {
            return null;
        }
        //token中解析from
        String from = tokenKey.startsWith(Constants.FROM_PC) ? Constants.FROM_PC : Constants.FROM_MOBILE;

        HewieUser hewieUser = parseUserByToken(tokenKey);
        if (hewieUser == null) {
            // 解析出错，过期了
            //1 mysql查询refreshToken
            RefreshToken refreshToken = null;
            if (Constants.FROM_PC.equals(from)) {
                refreshToken = refreshTokenDao.findOneByTokenKey(tokenKey);
            } else if (Constants.FROM_MOBILE.equals(from)) {
                refreshToken = refreshTokenDao.findOneByMobileTokenKey(tokenKey);
            }
            //2 如果不存在，表示当前访问没有登录，提示用户登录
            if (refreshToken == null) {
                return null;
            }
            // 3如果存在，就解析refreshToken
            try {
                JwtUtil.parseJWT(refreshToken.getRefreshToken());
                // 5如果refreshToken有效，创建新的token，和新的refreshToken
                String userId = refreshToken.getUserId();
                HewieUser userFromDb = userDao.findOneById(userId);
                String newTokenKey = createToken(response, userFromDb, from);
                log.info("newTokenKey ==> " + newTokenKey);

                return parseUserByToken(newTokenKey);
            } catch (Exception exception) {
                //4 如果refreshToken过期了，表示未登录
                refreshTokenDao.deleteAllByTokenKey(tokenKey);
                return null;
            }
        }
        return hewieUser;
    }

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @Override
    public ResponseResult getUserInfo(String userId) {
        //从数据库获取
        HewieUser userFromDb = userDao.findOneById(userId);
        // 判断是否存在
        if (userFromDb == null) {
            return ResponseResult.FAILED("用户不存在");
        }
        // 若存在，复制对象，清空密码、ip
        String userJson = gson.toJson(userFromDb);
        HewieUser hewieUser = gson.fromJson(userJson, HewieUser.class);
        hewieUser.setPassword("");
        hewieUser.setLoginIp("");
        hewieUser.setRegIp("");
        // 返回结果
        return ResponseResult.SUCCESS("获取用户信息成功").setData(hewieUser);
    }

    /**
     * 校验email是否已经存在
     * @param email
     * @return
     */
    @Override
    public ResponseResult checkEmail(String email) {
        HewieUser userFromDb = userDao.findOneByEmail(email);
        return userFromDb == null ? ResponseResult.FAILED("邮箱未注册") : ResponseResult.SUCCESS("邮箱已注册");
    }

    @Override
    public ResponseResult checkUserName(String userName) {
        HewieUser user = userDao.findOneByUserName(userName);

        return user == null ? ResponseResult.FAILED("用户名未注册") : ResponseResult.SUCCESS("用户名已注册");
    }

    /**
     * 修改用户信息
     * @param userId
     * @param hewieUser
     * @return
     */
    @Override
    public ResponseResult updateUserInfo(String userId, HewieUser hewieUser) {
        HewieUser userFromTokenKey = checkHewieUser();
        if (userFromTokenKey == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        if (!userFromTokenKey.getId().equals(userId)) {
            return ResponseResult.PERMISSION_DENIED();
        }
        HewieUser userAccount = userDao.findOneById(userFromTokenKey.getId());
        // 可以修改：头像、签名、用户名
        String userName = hewieUser.getUserName();
        if (!TextUtils.isEmpty(userName) && !userName.equals(userFromTokenKey.getUserName())) {
            HewieUser userFromDb = userDao.findOneByUserName(hewieUser.getUserName());
            if (userFromDb != null) {
                return ResponseResult.FAILED("该用户名已经注册");
            }
            userAccount.setUserName(hewieUser.getUserName());
        }
        if (!TextUtils.isEmpty(hewieUser.getAvatar())) {
            userAccount.setAvatar(hewieUser.getAvatar());
        }
        userAccount.setSign(hewieUser.getSign());
        userAccount.setUpdateTime(new Date());

        userDao.save(userAccount);

        // 干掉redis的token
        String tokenKey = CookieUtils.getCookie(getRequest(), Constants.User.KEY_COOKIE_TOKEN);
        redisUtil.del(Constants.User.KEY_TOKEN + tokenKey);

        return ResponseResult.SUCCESS("修改用户信息成功");
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes.getRequest();
    }

    private HttpServletResponse getResponse() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes.getResponse();
    }

    /**
     * 删除用户:修改用户状态
     *
     * 需要管理员权限
     *
     * @param userId
     * @return
     */
    @Override
    public ResponseResult deleteUserById(String userId) {
        int result = userDao.deleteUserByState(userId);
        if (result > 0) {
            return ResponseResult.SUCCESS("删除成功");
        }

        return ResponseResult.FAILED("用户不存在");
    }

    @Autowired
    private UserNoPasswordDao userNoPasswordDao;

    /**
     * 获取用户列表
     * @param page
     * @param size
     * @return
     */
    @Override
    public ResponseResult listUsers(int page, int size, String userName, String email) {

        //分页查询
        page = checkPage(page);
        size = checkSize(size);

        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<HewieUserNoPassword> all = userNoPasswordDao.findAll(new Specification<HewieUserNoPassword>() {
            @Override
            public Predicate toPredicate(Root<HewieUserNoPassword> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                List<Predicate> predicateList = new ArrayList<>();
                if (!TextUtils.isEmpty(userName)) {
                    Predicate userNamePre = cb.like(root.get("userName").as(String.class), "%" + userName + "%");
                    predicateList.add(userNamePre);
                }
                if (!TextUtils.isEmpty(email)) {
                    Predicate emailPre = cb.equal(root.get("email").as(String.class), email);
                    predicateList.add(emailPre);
                }
                Predicate[] preArray = new Predicate[predicateList.size()];
                predicateList.toArray(preArray);
                return cb.and(preArray);
            }
        }, pageable);

        return ResponseResult.SUCCESS("获取用户信息列表成功").setData(all);
    }

    /**
     * 修改密码
     * @param verifyCode
     * @param hewieUser
     * @return
     */
    @Override
    public ResponseResult updateUserPassword(String verifyCode, HewieUser hewieUser) {
        //检查邮箱
        String email = hewieUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            return ResponseResult.FAILED("邮箱不能为空");
        }
        // 根据邮箱去redis里获取验证码
        String verifyCodeInRedis = (String) redisUtil.get(Constants.User.KEY_EMAIL_CONTENT + email);
        if (verifyCodeInRedis == null || !verifyCodeInRedis.equals(verifyCode)) {
            return ResponseResult.FAILED("验证码错误");
        }
        redisUtil.del(Constants.User.KEY_EMAIL_CONTENT + email);
        //修改
        int result = userDao.updatePasswordByEmail(bCryptPasswordEncoder.encode(hewieUser.getPassword()), email);

        return result > 0 ? ResponseResult.SUCCESS("修改密码成功") : ResponseResult.FAILED("修改密码失败");
    }

    /**
     * 修改邮箱
     * @param email
     * @param verifyCode
     * @return
     */
    @Override
    public ResponseResult updateEmail(String email, String verifyCode) {
        HewieUser hewieUser = this.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        String verifyCodeInRedis = (String) redisUtil.get(Constants.User.KEY_EMAIL_CONTENT + email);
        if (verifyCodeInRedis == null || !verifyCodeInRedis.equals(verifyCode)) {
            return ResponseResult.FAILED("验证码不正确");
        }
        redisUtil.del(Constants.User.KEY_EMAIL_CONTENT + email);
        int result = userDao.updateEmailById(email, hewieUser.getId());
        return result > 0 ? ResponseResult.SUCCESS("修改邮箱成功") : ResponseResult.FAILED("修改邮箱失败");
    }

    /**
     * 退出登录
     * @return
     */
    @Override
    public ResponseResult doLogout() {
        //获取tokenKey
        String tokenKey = CookieUtils.getCookie(getRequest(), Constants.User.KEY_COOKIE_TOKEN);
        if (TextUtils.isEmpty(tokenKey)) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        redisUtil.del(Constants.User.KEY_TOKEN + tokenKey);
        //不删
        //refreshTokenDao.deleteAllByTokenKey(tokenKey);
        if (tokenKey.startsWith(Constants.FROM_PC)) {
            refreshTokenDao.deletePcTokenKey(tokenKey);
        } else if (tokenKey.startsWith(Constants.FROM_MOBILE)) {
            refreshTokenDao.deleteMobileTokenKey(tokenKey);
        }
        CookieUtils.deleteCookie(getResponse(), Constants.User.KEY_COOKIE_TOKEN);
        return ResponseResult.SUCCESS("退出登录成功");
    }

    @Override
    public ResponseResult getPcLoginQrCode() {
        String lastLoginId = CookieUtils.getCookie(getRequest(), Constants.User.KEY_LAST_REQUEST_LOGIN_ID);
//        if (!TextUtils.isEmpty(lastLoginId)) {
//            //检查上一次请求时间，如果太频繁
//            Object lastGetTime = redisUtil.get(Constants.User.KEY_LAST_REQUEST_LOGIN_ID + lastLoginId);
//            if (lastGetTime != null) {
//                return ResponseResult.FAILED("服务器繁忙，请稍候重试");
//            }
//            // 把redis删除
//            redisUtil.del(Constants.User.KEY_PC_LOGIN_ID + lastLoginId);
//        }
        //生成一个唯一的id
        long code;
        if (!TextUtils.isEmpty(lastLoginId)) {
            code = Long.parseLong(lastLoginId);
        } else {
            code = idWorker.nextId();
        }
        //保存到redis，值为false，时间为5分钟（二维码有效期）
        redisUtil.set(Constants.User.KEY_PC_LOGIN_ID + code,
                           Constants.User.KEY_PC_LOGIN_STATE_FALSE,
                     Constants.TimeValueInSecond.MIN * 5);
        //返回结果
        Map<String, Object> result = new HashMap<>();
        String originalDomain = TextUtils.getDomain(getRequest());
        result.put("code", String.valueOf(code));
        result.put("url", originalDomain + "/portal/image/qr-code/" + code);
        CookieUtils.setUpCookie(getResponse(), Constants.User.KEY_LAST_REQUEST_LOGIN_ID, String.valueOf(code));
        //redisUtil.set(Constants.User.KEY_LAST_REQUEST_LOGIN_ID + String.valueOf(code), "true", Constants.TimeValueInSecond.TEN_SEC);
        return ResponseResult.SUCCESS("获取成功").setData(result);
    }

    @Autowired
    private CountDownLatchManager countDownLatchManager;

    /**
     * 检查二维码的登录状态
     * 结果：
     * 1、登录成功（loginId对应的值为userId）
     * 2、等待扫描（false）
     * 3、二维码过期，loginId为空
     * @param loginId
     * @return
     */
    @Override
    public ResponseResult checkQrCodeLoginState(String loginId) {
        ResponseResult x = checkLoginState(loginId);
        if (x != null) return x;
        //先等待一段时间，再去检查，如果超时，返回等待扫码
        Callable<ResponseResult> callable = new Callable<ResponseResult>() {
            @Override
            public ResponseResult call() throws Exception {
                try {
                    //先阻塞
                    countDownLatchManager.getLatch(loginId).await(Constants.User.QR_CODE_STATE_CHECK_WAITING_TIME,
                            TimeUnit.SECONDS);
                    //收到状态更新的通知，就检查对应loginId的状态
                    ResponseResult result = checkLoginState(loginId);
                    if (result != null) return result;
                    //超时则返回等待扫描
                    return ResponseResult.WAITING_FOR_SCAN();
                } finally {
                    //完事后，删除对应的latch
                    countDownLatchManager.deleteLatch(loginId);
                }

            }
        };
        try {
            return callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseResult.WAITING_FOR_SCAN();
    }

    /**
     * 更新二維碼的登錄狀態
     * @param loginId
     * @return
     */
    @Override
    public ResponseResult updateQrCodeLoginState(String loginId) {
        //1、檢查用户是否登录
        HewieUser hewieUser = checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        //2、改变loginId对应的登录值true
        redisUtil.set(Constants.User.KEY_PC_LOGIN_ID + loginId, hewieUser.getId());
        //2.1 通知正在等待的扫码任务
        countDownLatchManager.onPhoneDoLogin(loginId);

        //3、返回结果
        return ResponseResult.SUCCESS("登录成功");
    }

    @Override
    public ResponseResult parseToken() {
        HewieUser hewieUser = checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        return ResponseResult.SUCCESS("获取成功").setData(hewieUser);
    }

    @Override
    public ResponseResult resetPassword(String userId, String password) {
        HewieUser userFromDb = userDao.findOneById(userId);
        if (userFromDb == null) {
            return ResponseResult.FAILED("用户不存在");
        }
        String encodePassword = bCryptPasswordEncoder.encode(password);
        userFromDb.setPassword(encodePassword);
        userFromDb.setUpdateTime(new Date());
        userDao.save(userFromDb);
        return ResponseResult.SUCCESS("重置密码成功");
    }

    @Override
    public ResponseResult getRegisterCount() {
        long count = userDao.count();
        return ResponseResult.SUCCESS("获取用户总数成功").setData(count);
    }

    @Override
    public ResponseResult checkEmailCode(String email, String emailCode, String captchaCode) {
        String captchaId = CookieUtils.getCookie(getRequest(), Constants.User.KEY_LAST_CAPTCHA_ID);
        String captchaVal = (String) redisUtil.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaId);
        if (!captchaCode.equals(captchaVal)){
            return ResponseResult.FAILED("人类验证码不正确");
        }
        String verifyCodeInRedis = (String) redisUtil.get(Constants.User.KEY_EMAIL_CONTENT + email);
        if (verifyCodeInRedis == null || !verifyCodeInRedis.equals(emailCode)) {
            return ResponseResult.FAILED("验证码错误");
        }
        return ResponseResult.SUCCESS("验证码正确");
    }

    private ResponseResult checkLoginState(String loginId) {
        String loginState = (String) redisUtil.get(Constants.User.KEY_PC_LOGIN_ID + loginId);
        if (loginState == null) {
            return ResponseResult.QR_CODE_DEPRECATE();
        }

        //创建token，登录
        if (!TextUtils.isEmpty(loginState) && !Constants.User.KEY_PC_LOGIN_STATE_FALSE.equals(loginState)) {
            HewieUser userFromDb = userDao.findOneById(loginState);
            if (userFromDb == null) {
                return ResponseResult.QR_CODE_DEPRECATE();
            }

            CookieUtils.deleteCookie(getResponse(), Constants.User.KEY_LAST_REQUEST_LOGIN_ID);
            createToken(getResponse(), userFromDb, Constants.FROM_PC);
            return ResponseResult.LOGIN_SUCCESS();
        }
        return null;
    }

    private String parseFrom(String tokenKey) {
        String token = (String) redisUtil.get(Constants.User.KEY_TOKEN + tokenKey);
        if (token != null) {
            try {
                Claims claims = JwtUtil.parseJWT(token);
                return ClaimsUtils.getFrom(claims);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private HewieUser parseUserByToken(String tokenKey) {
        String token = (String) redisUtil.get(Constants.User.KEY_TOKEN + tokenKey);
        if (token != null) {
            try {
                Claims claims = JwtUtil.parseJWT(token);
                return ClaimsUtils.claims2User(claims);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
