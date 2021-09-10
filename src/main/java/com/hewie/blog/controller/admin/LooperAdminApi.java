package com.hewie.blog.controller.admin;

import com.hewie.blog.pojo.Looper;
import com.hewie.blog.response.ResponseResult;
import com.hewie.blog.service.ILooperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/loop")
public class LooperAdminApi {

    @Autowired
    private ILooperService looperService;

    @PreAuthorize("@permission.admin()")
    @PostMapping
    public ResponseResult addLoop(@RequestBody Looper looper) {
        return looperService.addLoop(looper);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{loopId}")
    public ResponseResult deleteLoop(@PathVariable("loopId") String loopId) {
        return looperService.deleteLoop(loopId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/{loopId}")
    public ResponseResult updateLoop(@PathVariable("loopId") String loopId, @RequestBody Looper looper) {
        return looperService.updateLoop(loopId, looper);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/{loopId}")
    public ResponseResult getLoop(@PathVariable("loopId") String loopId) {
        return looperService.getLoop(loopId);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/list")
    public ResponseResult listLoops() {
        return looperService.listLoops();
    }
}
