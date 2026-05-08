package com.example.jianlou.common;

import lombok.Data;

// 统一返回格式（兼容前端）
@Data
public class ResultUtil {
    private String code; // success/failed
    private Object data;

    public static ResultUtil success() {
        ResultUtil result = new ResultUtil();
        result.setCode("success");
        return result;
    }

    public static ResultUtil success(Object data) {
        ResultUtil result = new ResultUtil();
        result.setCode("success");
        result.setData(data);
        return result;
    }

    public static ResultUtil failed() {
        ResultUtil result = new ResultUtil();
        result.setCode("failed");
        return result;
    }

}