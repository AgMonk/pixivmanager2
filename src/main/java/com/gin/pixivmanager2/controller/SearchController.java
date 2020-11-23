package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.service.FileService;
import com.gin.pixivmanager2.service.IllustrationService;
import com.gin.pixivmanager2.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索
 *
 * @author bx002
 * @date 2020/11/12 11:37
 */
@RestController
@RequestMapping("search")
@Validated
@Slf4j
public class SearchController {
    private final SearchService searchService;
    private final IllustrationService illustrationService;
    private final FileService fileService;

    public SearchController(SearchService searchService, IllustrationService illustrationService, FileService fileService) {
        this.searchService = searchService;
        this.illustrationService = illustrationService;
        this.fileService = fileService;
    }

    @RequestMapping("info")
    public List<Illustration> info(Integer start, Integer end, String... keyword) {
        return searchService.search(Arrays.asList(keyword), start, end, null, false, false);
    }

    @RequestMapping("download")
    public void download(Integer start, Integer end, String keyword) {
        List<String> idList = searchService.search(Collections.singletonList(keyword), start, end, null, true, false).stream().map(Illustration::getId).collect(Collectors.toList());
        List<Illustration> details = illustrationService.findList(idList, 200);
        fileService.download(details, "搜索/" + keyword);
    }
}
