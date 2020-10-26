package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Illustration;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public interface FileService {

    void download(Illustration illustration, String type);

    void download(Collection<Illustration> illustrations, String type);

    /**
     * 删除文件
     *
     * @param pidCollection
     */
    void del(Collection<String> pidCollection, String type);

    /**
     * 归档文件
     *
     * @param pidCollection
     */
    void archive(Collection<String> pidCollection, String type);

    Map<String, File> getFileMap(String type);
}
