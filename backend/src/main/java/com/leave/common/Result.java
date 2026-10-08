package com.leave.common;

import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class Result<T> {

    /** 200 成功 / 400 业务错误 / 401 未登录 / 403 无权限 / 500 系统错误 */
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return build(200, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return build(200, "操作成功", data);
    }

    public static <T> Result<T> fail(String message) {
        return build(400, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return build(code, message, null);
    }

    public static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}
