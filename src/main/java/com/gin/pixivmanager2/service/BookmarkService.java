package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.TaskProgress;

import java.util.List;

/**
 * @author bx002
 */
public interface BookmarkService {

    void downloadUntaggedBookmarks();

    List<TaskProgress> getProgressesBmk();

    List<TaskProgress> getProgressesAddTags();
}
