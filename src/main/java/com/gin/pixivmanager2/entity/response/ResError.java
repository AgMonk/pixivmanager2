package com.gin.pixivmanager2.entity.response;

/**
 * 报错信息返回对象
 * @author bx002
 */
public class ResError extends Res<Void>{
    public ResError(int code, String message) {
        super(code, message);
    }
}
