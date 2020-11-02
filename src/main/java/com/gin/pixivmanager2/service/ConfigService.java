package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Config;
import com.gin.pixivmanager2.entity.NgaId;

import java.util.List;

/**
 * 配置
 */
public interface ConfigService {
    List<NgaId> getFids();

    List<NgaId> getTids();

    Config getCookie(String name);

    Config getPath(String name);

    List<Config> getKeywordList();

    Config getConfig(String name);


}
