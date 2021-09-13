package com.hewie.blog.service.impl;

import com.hewie.blog.dao.ImageDao;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.pojo.Image;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IImageService;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class ImageServiceImpl extends BaseService implements IImageService {

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private ImageDao imageDao;

    @Value("${hewie.blog.image.save-path}")
    public String imagePath;

    @Value("${hewie.blog.image.max-size}")
    public long imageMaxSize;

    @Autowired
    private IUserService userService;

    /**
     * 上传图片
     *
     * 上传的路径：可以配置
     * 上传的内容：命名：id，每天一个文件夹
     * 限制文件大小
     * 保存记录到数据库
     * ID/存储路径/url/原名称/用户id/状态/创建日期/更新日期
     * @param file
     * @return
     */
    @Override
    public ResponseResult uploadImage(String original, MultipartFile file) {
        // 判断是否有文件
        if (file == null) {
            return ResponseResult.FAILED("图片不可以未空");
        }
        // 判断文件类型：只支持图片（png、jpg、gif等）
        String contentType = file.getContentType();
        if (TextUtils.isEmpty(contentType)) {
            return ResponseResult.FAILED("图片格式错误");
        }
        log.info("contentType == >" + contentType);

        // 获取相关数据：文件类型、文件名称
        String originalFilename = file.getOriginalFilename();
        log.info("originalFilename == > " + originalFilename);
        String type = getImageType(contentType);
        if (type == null){
            return ResponseResult.FAILED("不支持此图片类型（支持png、jpg、gif、jpeg）");
        }


        //限制文件大小
        long size = file.getSize();
        log.info("imageMaxSize == > " + imageMaxSize + "---size == >" + size);
        if (size > imageMaxSize) {
            return ResponseResult.FAILED("图片最大仅支持" + (imageMaxSize/1024/1024) + "Mb");
        }
        //创建图片的保存目录
        //配置目录/日期/类型/id.类型
        long currentMillions = System.currentTimeMillis();
        String currentDay = new SimpleDateFormat("yyyy_MM_dd").format(currentMillions);
        log.info("currentDay == >" +currentDay);
        String dayPath = imagePath + File.separator + currentDay;
        File dayPathFile = new File(dayPath);
        //判断日期文件夹是否存在
        if (!dayPathFile.exists()) {
            dayPathFile.mkdirs();
        }
        String targetName = String.valueOf(idWorker.nextId());
        String targetPath = dayPath + File.separator + type + File.separator + targetName + "." + type;

        File targetFile = new File(targetPath);
        //判断类型文件夹是否存在
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        log.info("targetFile == >" + targetFile);
        // 保存文件
        try {
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            file.transferTo(targetFile);

            //返回结果：包含图片的名称和访问路径
            //1、访问路径-->对应着解析
            Map<String, String> result = new HashMap<>();
            String resultPath = currentMillions + "_" + targetName + "." + type;
            //2、名称
            result.put("id", resultPath);
            result.put("name", originalFilename);

            //保存到数据库
            Image image = new Image();
            image.setId(targetName);
            image.setContentType(contentType);
            image.setPath(targetFile.getPath());
            image.setCreateTime(new Date());
            image.setUpdateTime(new Date());
            image.setName(originalFilename);
            image.setUrl(resultPath);
            image.setOriginal(original);
            HewieUser hewieUser = userService.checkHewieUser();
            image.setUserId(hewieUser.getId());
            imageDao.save(image);

            return ResponseResult.SUCCESS("图片上传成功").setData(result);

        } catch (IOException e) {
            e.printStackTrace();
            //return ResponseResult.FAILED("图片上传失败");
        }
        return ResponseResult.FAILED("图片上传失败");
    }

    private String getImageType(String contentType) {
        String type = null;
        if (Constants.ImageType.TYPE_PNG_WITH_PREFIX.equals(contentType)) {
            type = Constants.ImageType.TYPE_PNG;
        } else if (Constants.ImageType.TYPE_JPG_WITH_PREFIX.equals(contentType)) {
            type = Constants.ImageType.TYPE_JPG;
        } else if (Constants.ImageType.TYPE_GIF_WITH_PREFIX.equals(contentType)) {
            type = Constants.ImageType.TYPE_GIF;
        } else if (Constants.ImageType.TYPE_JPEG_WITH_PREFIX.equals(contentType)) {
            type = Constants.ImageType.TYPE_JPEG;
        }
        return type;
    }


    private final Object mLock = new Object();
    /**
     * 查看图片
     * @param response
     * @return
     */
    @Override
    public void viewImage(HttpServletResponse response, String imageId) {
        //配置的目录已知
        String[] paths = imageId.split("_");
        //需要日期、类型、id：日期时间戳_ID.类型
        String dayValue = paths[0];
        String format = new SimpleDateFormat("yyyy_MM_dd").format(Long.parseLong(dayValue));
        String name = paths[1];
        log.info("name === >" + name);

        String[] res = name.split("\\.");
        String type = res[1];

        String targetPath = imagePath + File.separator + format + File.separator +
                            type + File.separator + name;
        log.info("get image path ===>" + targetPath);

        File file = new File(targetPath);
        OutputStream writer = null;
        FileInputStream fis = null;
        try {
            response.setContentType("image/jpeg");
            writer = response.getOutputStream();
            //读取
            fis = new FileInputStream(file);
            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = fis.read(buff)) != -1) {
                writer.write(buff, 0, len);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public ResponseResult listImages(int page, int size, String original) {
        //参数检查
        page = checkPage(page);
        size = checkSize(size);
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        HewieUser hewieUser = userService.checkHewieUser();
        if (hewieUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }

        Page<Image> all = imageDao.findAll(new Specification<Image>() {
            @Override
            public Predicate toPredicate(Root<Image> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                Predicate userPre = cb.equal(root.get("userId").as(String.class), hewieUser.getId());
                Predicate statePre = null;
                if (Constants.User.ROLE_NORMAL.equals(hewieUser.getRoles())){
                    statePre = cb.equal(root.get("state").as(String.class), "1");
                }
                Predicate and;
                if (statePre != null) {
                    if (!TextUtils.isEmpty(original)) {
                        Predicate originalPre = cb.equal(root.get("original").as(String.class), original);
                        and = cb.and(userPre, statePre, originalPre);
                    } else {
                        and = cb.and(userPre, statePre);
                    }
                } else {
                    if (!TextUtils.isEmpty(original)) {
                        Predicate originalPre = cb.equal(root.get("original").as(String.class), original);
                        and = cb.and(userPre, originalPre);
                    } else {
                        and = cb.and(userPre);
                    }
                }
                return and;
            }
        }, pageable);
        return ResponseResult.SUCCESS("获取图片列表成功").setData(all);
    }

    /**
     * 删除图片
     * 改变状态
     * @param imageId
     * @return
     */
    @Override
    public ResponseResult deleteImageById(String imageId) {
        int result = imageDao.deleteImageByState(imageId);
        return result > 0 ? ResponseResult.SUCCESS("删除图片成功") : ResponseResult.FAILED("刪除图片失败");
    }

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void createQrCode(String code, HttpServletResponse response, HttpServletRequest request) {
        //检查code是否过期
        String loginState = (String) redisUtil.get(Constants.User.KEY_PC_LOGIN_ID + code);
        if (TextUtils.isEmpty(loginState)) {
            //todo：返回一张图片显示已经过期
            return ;
        }

        String originalDomain = TextUtils.getDomain(request);
        String content = originalDomain + Constants.APP_DOWNLOAD_PATH + "===" + code;
        log.info("content == >" + content);
        byte[] result = QrCodeUtils.encodeQRCode(content);
        response.setContentType(QrCodeUtils.RESPONSE_CONTENT_TYPE);
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(result);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //http://localhost/portal/app===876823804096020480

    }

}
