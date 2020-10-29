package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.FanboxItem;
import com.gin.pixivmanager2.service.FanboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@RestController
@RequestMapping("fanbox")
@Validated
@Slf4j
public class FanboxController {

    private final FanboxService fanboxService;


    public FanboxController(FanboxService fanboxService) {
        this.fanboxService = fanboxService;
    }

    @RequestMapping("listCreator")
    public void listCreator(String creatorId) {
        fanboxService.listCreator(creatorId);
    }

    @RequestMapping("downloadItem")
    public void downloadItem(String postId) {
        fanboxService.downloadItem(postId);
    }

    @RequestMapping("listSupporting")
    public List<FanboxItem> listSupporting(@RequestParam(defaultValue = "20") Integer limit) {
        return fanboxService.listSupporting(limit).stream().limit(5).collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void autoFanbox() {
        listSupporting(20);
    }
}
