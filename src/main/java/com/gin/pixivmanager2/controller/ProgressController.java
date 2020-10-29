package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.TaskProgress;
import com.gin.pixivmanager2.entity.response.Res;
import com.gin.pixivmanager2.service.ProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("progress")
@Validated
@Slf4j
public class ProgressController {
    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @RequestMapping("get")
    public Res<List<TaskProgress>> get(@RequestParam(defaultValue = "") String type) {
        return new Res<>(2000, "进度获取成功", progressService.get(type));
    }
}
