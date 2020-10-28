package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.TaskProgress;
import com.gin.pixivmanager2.entity.response.Res;
import com.gin.pixivmanager2.service.BookmarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public void downloadUntagged() {
        bookmarkService.downloadUntaggedBookmarks();
    }

    @RequestMapping("getProgressesAddTags")
    public Res<List<TaskProgress>> getProgressesAddTags() {
        return new Res<>(2000, "正在添加TAG列表 获取成功", bookmarkService.getProgressesAddTags());
    }

    @RequestMapping("getProgressesBmk")
    public Res<List<TaskProgress>> getProgressesBmk() {
        return new Res<>(2000, "正在获取收藏作品 获取成功", bookmarkService.getProgressesBmk());
    }
}
