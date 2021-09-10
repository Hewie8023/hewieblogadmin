package com.hewie.blog.service.impl;

import com.hewie.blog.dao.LooperDao;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.pojo.Looper;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ILooperService;
import com.hewie.blog.service.IUserService;
import com.hewie.blog.utils.Constants;
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
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Transactional
public class LooperServiceImpl extends BaseService implements ILooperService {

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private LooperDao looperDao;

    @Autowired
    private IUserService userService;

    @Override
    public ResponseResult addLoop(Looper looper) {
        String title = looper.getTitle();
        if (TextUtils.isEmpty(title)) {
            return ResponseResult.FAILED("标题不能为空");
        }
        String imageUrl = looper.getImageUrl();
        if (TextUtils.isEmpty(imageUrl)) {
            return ResponseResult.FAILED("图片不能为空");
        }
        String targetUrl = looper.getTargetUrl();
        if (TextUtils.isEmpty(targetUrl)) {
            return ResponseResult.FAILED("跳转链接不能为空");
        }
        looper.setId(idWorker.nextId() + "");
        looper.setCreateTime(new Date());
        looper.setUpdateTime(new Date());

        looperDao.save(looper);
        return ResponseResult.SUCCESS("保存轮播图成功").setData(looper);
    }

    @Override
    public ResponseResult getLoop(String loopId) {
        Looper loop = looperDao.findOneById(loopId);
        if (loop == null) {
            return ResponseResult.FAILED("轮播图不存在");
        }
        return ResponseResult.SUCCESS("获取轮播图成功").setData(loop);
    }

    @Override
    public ResponseResult listLoops() {
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        HewieUser hewieUser = userService.checkHewieUser();
        List<Looper> loopList;
        if (hewieUser == null || !Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
            loopList = looperDao.listLoopsByState("1");
        } else {
            loopList = looperDao.findAll(sort);
        }
        return ResponseResult.SUCCESS("获取轮播图列表成功").setData(loopList);
    }

    @Override
    public ResponseResult updateLoop(String loopId, Looper looper) {
        Looper loopFromDb = looperDao.findOneById(loopId);
        if (loopFromDb == null) {
            return ResponseResult.FAILED("轮播图不存在");
        }
        String title = looper.getTitle();
        if (!TextUtils.isEmpty(title)) {
            loopFromDb.setTitle(title);
        }
        String imageUrl = looper.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            loopFromDb.setImageUrl(imageUrl);
        }
        String targetUrl = looper.getTargetUrl();
        if (!TextUtils.isEmpty(targetUrl)) {
            loopFromDb.setTargetUrl(targetUrl);
        }
        loopFromDb.setOrder(looper.getOrder());
        loopFromDb.setState(looper.getState());
        loopFromDb.setUpdateTime(new Date());
        looperDao.save(loopFromDb);
        return ResponseResult.SUCCESS("修改轮播图成功");
    }

    @Override
    public ResponseResult deleteLoop(String loopId) {
        looperDao.deleteById(loopId);
        return ResponseResult.SUCCESS("删除轮播图成功");
    }
}
