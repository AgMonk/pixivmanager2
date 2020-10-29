package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONObject;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.TaskProgress;
import com.gin.pixivmanager2.util.PixivPost;
import com.gin.pixivmanager2.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final String uid;
    private final String cookie;
    private final String tt;
    private final ThreadPoolTaskExecutor requestExecutor;

    List<TaskProgress> progressesBmk = new ArrayList<>();
    List<TaskProgress> progressesAddTags = new ArrayList<>();

    public BookmarkServiceImpl(ConfigService configService, ThreadPoolTaskExecutor requestExecutor) {
        cookie = configService.getCookie("pixiv").getValue();
        uid = configService.getConfig("pixivUid").getValue();
        tt = configService.getConfig("tt").getValue();

        this.requestExecutor = requestExecutor;
    }

    public List<Illustration> getBookmarks(String tag, Integer page) {
        TaskProgress taskProgress = new TaskProgress(tag);
        progressesBmk.add(taskProgress);

        List<JSONObject> jsonObjectList = PixivPost.getBookmarks(uid, cookie, tag, page, requestExecutor, taskProgress.getProgress());
        progressesBmk.removeIf(t -> t.getCreatedTime() == taskProgress.getCreatedTime());
        if (jsonObjectList == null) {
            return new ArrayList<>();
        }
        List<String> list = jsonObjectList.stream().map(j -> j.getString("id")).collect(Collectors.toList());
        IllustrationService illustrationService = SpringContextUtil.getBean(IllustrationService.class);

        return illustrationService.findList(list, 0);
    }

    @Override
    @Async("defaultExecutor")
    public void downloadUntaggedBookmarks() {
        List<Illustration> list = getBookmarks("未分類", 3);
        if (list.size() == 0) {
            return;
        }
        FileService fileService = SpringContextUtil.getBean(FileService.class);
        fileService.download(list, "未分类");

        //收藏
        TaskProgress taskProgress = new TaskProgress("添加Tag");
        progressesAddTags.add(taskProgress);
        Map<String, String> pidAndTags = new HashMap<>();
        list.forEach(i -> pidAndTags.put(i.getId(), i.getTagString()));
        PixivPost.addTags(pidAndTags, cookie, tt, requestExecutor, taskProgress.getProgress());
        progressesAddTags.removeIf(t -> t.getCreatedTime() == taskProgress.getCreatedTime());

    }

    @Override
    public List<TaskProgress> getProgressesBmk() {
        return progressesBmk;
    }

    @Override
    public List<TaskProgress> getProgressesAddTags() {
        return progressesAddTags;
    }
}
