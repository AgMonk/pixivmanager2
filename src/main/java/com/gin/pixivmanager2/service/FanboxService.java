package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.FanboxItem;

import java.util.List;

public interface FanboxService {


    List<FanboxItem> listCreator(String creatorId);

    List<FanboxItem> listSupporting();

    void downloadItem(String postId);
}
