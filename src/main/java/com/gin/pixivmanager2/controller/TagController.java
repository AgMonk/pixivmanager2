package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.Tag;
import com.gin.pixivmanager2.entity.response.Res;
import com.gin.pixivmanager2.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("tag")
@Validated
@Slf4j
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }


    @RequestMapping("count")
    public Res<List<Tag>> count(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") Boolean all) {
        List<Tag> count = tagService.count((page - 1) * 20, keyword.toLowerCase(), 20, all);
        return new Res<>(2000, "统计Tag成功", count);
    }

}
