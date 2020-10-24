package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.util.PixivPost;
import com.gin.pixivmanager2.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final String uid;
    private final String cookie;
    private final ThreadPoolTaskExecutor requestExecutor;

    public BookmarkServiceImpl(ConfigService configService, ThreadPoolTaskExecutor requestExecutor) {
        cookie = configService.getCookie("pixiv").getValue();
        uid = configService.getConfig("pixivUid").getValue();

        this.requestExecutor = requestExecutor;
    }

    public List<Illustration> getBookmarks(String tag, Integer page) {
        List<String> list = PixivPost.getBookmarks(uid, cookie, tag, page, requestExecutor, null)
                .stream().map(j -> j.getString("id")).collect(Collectors.toList());
        IllustrationService illustrationService = SpringContextUtil.getBean(IllustrationService.class);
        return illustrationService.findList(list, 0);
    }

    public List<Illustration> getUntaggedBookmarks() {
        return getBookmarks("未分類", 1);
    }
}
