package com.gin.pixivmanager2.entity.response;

import lombok.Data;

/**
 * 标准响应对象
 */
@Data
public class Res<T> {
    int code;
    String message;
    T data;


    
    public Res() {
    }

    public Res(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Res(String message, T data) {
        this.code = 65535;
        this.message = message;
        this.data = data;
    }

    public Res(String message) {
        this.code = 65535;
        this.message = message;
    }

    public Res(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
