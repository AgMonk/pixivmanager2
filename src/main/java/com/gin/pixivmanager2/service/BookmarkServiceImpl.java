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
    private final String tt;
    private final ThreadPoolTaskExecutor requestExecutor;

    public BookmarkServiceImpl(ConfigService configService, ThreadPoolTaskExecutor requestExecutor) {
        cookie = configService.getCookie("pixiv").getValue();
        uid = configService.getConfig("pixivUid").getValue();
        tt = configService.getConfig("tt").getValue();

        this.requestExecutor = requestExecutor;
    }

    public List<Illustration> getBookmarks(String tag, Integer page) {
        List<String> list = PixivPost.getBookmarks(uid, cookie, tag, page, requestExecutor, null)
                .stream().map(j -> j.getString("id")).collect(Collectors.toList());
        IllustrationService illustrationService = SpringContextUtil.getBean(IllustrationService.class);
        return illustrationService.findList(list, 0);
    }

    public void downloadUntaggedBookmarks() {
        List<Illustration> list = getBookmarks("未分類", 3);
        FileService fileService = SpringContextUtil.getBean(FileService.class);
        fileService.download(list, "未分类");

        //收藏
//        Map<String, String> pidAndTags = new HashMap<>();
//        list.forEach(i -> pidAndTags.put(i.getId(), i.getTagString()));
//        PixivPost.addTags(pidAndTags, cookie, tt, requestExecutor, null);

    }
}
