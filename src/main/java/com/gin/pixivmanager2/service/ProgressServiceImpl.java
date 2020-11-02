package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.TaskProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@Service
@Slf4j
public class ProgressServiceImpl implements ProgressService {
    private final List<TaskProgress> list = new ArrayList<>();

    @Override
    public TaskProgress add(String type) {
        TaskProgress taskProgress = new TaskProgress(type);
        synchronized (list) {
            list.add(taskProgress);
        }
        return taskProgress;
    }

    @Override
    public boolean remove(TaskProgress taskProgress) {
        synchronized (list) {
            return list.removeIf(t -> t.getId().equals(taskProgress.getId()));
        }
    }

    @Override
    public List<TaskProgress> get(String type) {
        return list.stream().filter(t -> t.getType().contains(type)).collect(Collectors.toList());
    }

}
