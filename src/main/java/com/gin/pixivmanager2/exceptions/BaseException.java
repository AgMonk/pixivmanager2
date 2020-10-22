package com.gin.pixivmanager2.exceptions;

import lombok.Data;

/**
 * 基础异常
 *
 * @author bx002
 */
@Data
public class BaseException extends RuntimeException {
    int code;
    String message;

    public BaseException() {
    }

    public BaseException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public BaseException(int code, RuntimeException e) {
        this.code = code;
        this.message = e.getLocalizedMessage();
    }

    public BaseException(IExceptions iExceptions) {
        this.code = iExceptions.getCode();
        this.message = iExceptions.getMessage();
    }
}
