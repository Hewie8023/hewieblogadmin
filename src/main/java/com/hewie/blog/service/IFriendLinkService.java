package com.hewie.blog.service;

import com.hewie.blog.pojo.FriendLink;
import com.hewie.blog.response.ResponseResult;

public interface IFriendLinkService {
    ResponseResult addFriendLink(FriendLink friendLink);

    ResponseResult getFriendLink(String friendLinkId);

    ResponseResult listFriendLinks();

    ResponseResult deleteFriendLink(String friendLinkId);


    ResponseResult updateFriendLink(String friendLinkId, FriendLink friendLink);
}
