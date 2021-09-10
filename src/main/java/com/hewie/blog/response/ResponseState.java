package com.hewie.blog.response;

public enum ResponseState {
    SUCCESS(true, 2000, "操作成功"),
    LOGIN_SUCCESS(true, 2001, "登录成功"),
    JOIN_SUCCESS(true, 2002, "注册成功"),
    FAILED(false, 4000, "操作失败"),
    GET_RESOURCE_FAILED(false, 4001, "获取资源失败"),
    ACCOUNT_NOT_LOGIN(false, 4002, "账户未登录"),
    PERMISSION_DENIED(false, 4003, "无权限"),
    ACCOUNT_DENIED(false, 4004, "账户被禁用"),
    ERROR_404(false, 4005, "页面丢失"),
    ERROR_403(false, 4006, "权限不足"),
    ERROR_504(false, 4007, "系统繁忙，请稍候重试"),
    ERROR_505(false, 4008, "请求错误，请检查"),
    WAITING_FOR_SCAN(false, 4009, "等待扫码"),
    QR_CODE_DEPRECATE(false, 4010, "二维码过期"),
    LOGIN_FAILED(false, 4999, "登录失败");

    private int code;
    private String message;
    private boolean success;

    ResponseState(boolean success, int code, String message) {
        this.code = code;
        this.message = message;
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
