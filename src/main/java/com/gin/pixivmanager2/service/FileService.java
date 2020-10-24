package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Illustration;

import java.util.Collection;

public interface FileService {

    void download(Illustration illustration, String type);

    void download(Collection<Illustration> illustrations, String type);

}
