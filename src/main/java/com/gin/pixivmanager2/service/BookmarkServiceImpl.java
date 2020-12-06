package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONObject;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.TaskProgress;
import com.gin.pixivmanager2.util.PixivPost;
import com.gin.pixivmanager2.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@Slf4j
@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final String uid;
    private final String cookie;
    private final String tt;
    private final ThreadPoolTaskExecutor requestExecutor;
    private final ProgressService progressService;
    private final FileService fileService;

    public BookmarkServiceImpl(ConfigService configService, ThreadPoolTaskExecutor requestExecutor, ProgressService progressService, FileService fileService) {
        cookie = configService.getCookie("pixiv").getValue();
        uid = configService.getConfig("pixivUid").getValue();
        tt = configService.getConfig("tt").getValue();

        this.requestExecutor = requestExecutor;
        this.progressService = progressService;
        this.fileService = fileService;
    }

    public List<Illustration> getBookmarks(String tag, Integer page) {
        TaskProgress taskProgress = progressService.add(tag);
        List<JSONObject> jsonObjectList = PixivPost.getBookmarks(uid, cookie, tag, page, requestExecutor, taskProgress.getProgress());
        progressService.remove(taskProgress);
        if (jsonObjectList == null) {
            return new ArrayList<>();
        }
        List<String> list = jsonObjectList.stream().map(j -> j.getString("id")).collect(Collectors.toList());
        IllustrationService illustrationService = SpringContextUtil.getBean(IllustrationService.class);

        return illustrationService.findList(list, 0);
    }

    @Override
    @Scheduled(cron = "0 5/10 * * * ?")
    public void downloadUntaggedBookmarks() {
        List<Illustration> list = getBookmarks("未分類", 3);
        if (list.size() > 0) {
            fileService.download(list, "未分类");
            //收藏
            TaskProgress taskProgress = progressService.add("添加Tag");
            Map<String, String> pidAndTags = new HashMap<>();
            list.forEach(i -> pidAndTags.put(i.getId(), i.getTagString()));
            PixivPost.addTags(pidAndTags, cookie, tt, requestExecutor, taskProgress.getProgress());
            progressService.remove(taskProgress);
        }
//        fileService.startDownload();
    }

}
