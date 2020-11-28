package com.gin.pixivmanager2.service;

/**
 * Aira2下载业务
 *
 * @author bx002
 * @date 2020/11/28 14:42
 */
public interface Aira2Service {
    boolean isAira2Active();

    boolean addUriPixiv(String url);
}
