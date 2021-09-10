package com.hewie.blog.service.impl;

import com.hewie.blog.dao.FriendLinkDao;
import com.hewie.blog.pojo.FriendLink;
import com.hewie.blog.pojo.HewieUser;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.IFriendLinkService;
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
public class FriendLinkServiceImpl extends BaseService implements IFriendLinkService {

    @Autowired
    private SnowflakeIdWorker idWorker;

    @Autowired
    private FriendLinkDao friendLinkDao;

    @Autowired
    private IUserService userService;

    @Override
    public ResponseResult addFriendLink(FriendLink friendLink) {
        String name = friendLink.getName();
        if (TextUtils.isEmpty(name)) {
            return ResponseResult.FAILED("友情链接名字不能为空");
        }
        String logo = friendLink.getLogo();
        if (TextUtils.isEmpty(logo)) {
            return ResponseResult.FAILED("友情链接的logo不能为空");
        }
        String url = friendLink.getUrl();
        if (TextUtils.isEmpty(url)) {
            return ResponseResult.FAILED("友情链接url不能为空");
        }
        friendLink.setId(idWorker.nextId() + "");
        friendLink.setCreateTime(new Date());
        friendLink.setUpdateTime(new Date());
        friendLinkDao.save(friendLink);
        return ResponseResult.SUCCESS("添加友情链接成功");
    }

    @Override
    public ResponseResult getFriendLink(String friendLinkId) {
        FriendLink oneById = friendLinkDao.findOneById(friendLinkId);
        if (oneById == null) {
            return ResponseResult.FAILED("该友情链接不存在");
        }
        return ResponseResult.SUCCESS("获取友情链接成功").setData(oneById);
    }

    @Override
    public ResponseResult listFriendLinks() {

        Sort sort = new Sort(Sort.Direction.DESC, "order", "createTime");
        HewieUser hewieUser = userService.checkHewieUser();
        List<FriendLink> friendLinks;
        if (hewieUser == null || !Constants.User.ROLE_ADMIN.equals(hewieUser.getRoles())) {
            friendLinks = friendLinkDao.listFriendLinksByState("1");
        } else {
            friendLinks = friendLinkDao.findAll(sort);
        }
        return ResponseResult.SUCCESS("获取友情链接列表成功").setData(friendLinks);
    }

    @Override
    public ResponseResult deleteFriendLink(String friendLinkId) {
        int result = friendLinkDao.deleteAllById(friendLinkId);
        return result > 0 ? ResponseResult.SUCCESS("删除友情链接成功") : ResponseResult.FAILED("删除友情链接失败");
    }

    @Override
    public ResponseResult updateFriendLink(String friendLinkId, FriendLink friendLink) {
        FriendLink friendLinkFromDb = friendLinkDao.findOneById(friendLinkId);
        if (friendLinkFromDb == null) {
            return ResponseResult.FAILED("友情链接不存在");
        }
        String name = friendLink.getName();
        if (!TextUtils.isEmpty(name)) {
            friendLinkFromDb.setName(name);
        }
        String logo = friendLink.getLogo();
        if (!TextUtils.isEmpty(logo)) {
            friendLinkFromDb.setLogo(logo);
        }
        String url = friendLink.getUrl();
        if (!TextUtils.isEmpty(url)) {
            friendLinkFromDb.setUrl(url);
        }

        String state = friendLink.getState();
        if (!TextUtils.isEmpty(state)) {
            friendLinkFromDb.setState(state);
        }

        friendLinkFromDb.setOrder(friendLink.getOrder());
        friendLinkFromDb.setUpdateTime(new Date());
        friendLinkDao.save(friendLinkFromDb);
        return ResponseResult.SUCCESS("修改友情链接成功");
    }
}
