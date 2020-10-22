package com.gin.pixivmanager2.exceptions;

import lombok.Data;

/**
 * 用户异常
 *
 * @author bx002
 */
@Data
public class BusinessException extends BaseException {

    public BusinessException(IExceptions iExceptions) {
        super(iExceptions);
    }


    public BusinessException(int code, String message) {
        super(code, message);
    }


    public BusinessException(IExceptions iExceptions, String msg) {
        super(iExceptions);
        this.message += " " + msg;
    }
}
