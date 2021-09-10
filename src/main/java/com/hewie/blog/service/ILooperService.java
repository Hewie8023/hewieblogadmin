package com.hewie.blog.service;

import com.hewie.blog.pojo.Looper;
import com.hewie.blog.response.ResponseResult;

public interface ILooperService {
    ResponseResult addLoop(Looper looper);

    ResponseResult getLoop(String loopId);

    ResponseResult listLoops();

    ResponseResult updateLoop(String loopId, Looper looper);

    ResponseResult deleteLoop(String loopId);

}
