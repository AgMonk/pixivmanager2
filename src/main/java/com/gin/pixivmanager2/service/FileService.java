package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Illustration;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public interface FileService {

    void download(Illustration illustration, String type);

    void download(Collection<Illustration> illustrations, String type);

    Map<String, File> getFileMap(String type);
}
