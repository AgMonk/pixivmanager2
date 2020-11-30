package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.DownloadingFile;
import com.gin.pixivmanager2.entity.FanboxItem;
import com.gin.pixivmanager2.entity.Illustration;

import java.io.File;
import java.util.Collection;
import java.util.List;
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

    void download(FanboxItem fanboxItem);

    void download(List<FanboxItem> fanboxItemList);

    Map<String, File> getFileMap(String type);

    void addRepostQueue(Collection<String> pidCollection, String type);

    List<DownloadingFile> getDownloadingFileList();

    void download();
}
