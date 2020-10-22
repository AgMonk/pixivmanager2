package com.gin.pixivmanager2.entity.response;

/**
 * 带分页数据的返回对象
 *
 * @author bx002
 */
public class ResPage<T> extends Res<PageData<T>> {

    public ResPage(int code, String message, PageData<T> data) {
        super(code, message, data);
    }
}
