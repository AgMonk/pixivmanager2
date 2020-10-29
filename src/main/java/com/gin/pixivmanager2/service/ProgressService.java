package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.TaskProgress;

import java.util.List;

public interface ProgressService {

    TaskProgress add(String type);

    boolean remove(TaskProgress taskProgress);

    List<TaskProgress> get(String type);
}
