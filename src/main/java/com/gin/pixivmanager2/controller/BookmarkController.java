package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.service.BookmarkService;
import lombok.extern.slf4j.Slf4j;
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

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @RequestMapping("downloadUntagged")
//    @Scheduled(cron = "0 0/10 * * * ?")
    public void downloadUntagged() {
        bookmarkService.downloadUntaggedBookmarks();
    }

}
