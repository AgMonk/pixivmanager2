package com.gin.pixivmanager2.entity.response;

import lombok.Data;

/**
 * 分页数据
 *
 * @author bx002
 */
@Data
public class PageData<T> {
    /**
     * 总数
     */
    final int totalCount;
    /**
     * 总页数
     */
    final int pageCount;
    /**
     * 当前页
     */
    final int page;

    final int pageSize;

    T data;


    public PageData(int totalCount, int pageCount, int page, int pageSize, T data) {
        this.totalCount = totalCount;
        this.pageCount = pageCount;
        this.page = page;
        this.pageSize = pageSize;
        this.data = data;
    }

}
