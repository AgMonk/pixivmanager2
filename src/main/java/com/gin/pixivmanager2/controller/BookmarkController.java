package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.service.BookmarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author bx002
 */
@RestController
@RequestMapping("bmk")
@Validated
@Slf4j
public class BookmarkController {
    private final BookmarkService bookmarkService;
    private final ThreadPoolTaskExecutor queueExecutor;

    public BookmarkController(BookmarkService bookmarkService, ThreadPoolTaskExecutor queueExecutor) {
        this.bookmarkService = bookmarkService;
        this.queueExecutor = queueExecutor;
    }

    @RequestMapping("downloadUntagged")
    public void downloadUntagged() {
        queueExecutor.execute(bookmarkService::downloadUntaggedBookmarks);
        log.info("下载未分类 加入队列");
    }

}
