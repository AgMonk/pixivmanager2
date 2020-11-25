package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONObject;
import com.gin.pixivmanager2.entity.Config;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.TaskProgress;
import com.gin.pixivmanager2.util.PixivPost;
import com.gin.pixivmanager2.util.SpringContextUtil;
import com.gin.pixivmanager2.util.TasksUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@Slf4j
@Transactional
@Service

public class SearchServiceImpl implements SearchService {
    private final String cookie;
    private List<Config> keywordList;
    private final ThreadPoolTaskExecutor requestExecutor;
    private final ConfigService configService;

    public SearchServiceImpl(ConfigService configService, ThreadPoolTaskExecutor requestExecutor) {
        this.configService = configService;
        cookie = configService.getCookie("pixiv").getValue();
        this.requestExecutor = requestExecutor;

    }


    @Override
    public List<Illustration> search(String keyword, Integer p, String mode, boolean notBmkOnly, boolean searchTitle) {
        JSONObject result = PixivPost.search(keyword, p, cookie, searchTitle, mode);
        return result.getJSONArray("data").stream()
                .map(j -> Illustration.parse((JSONObject) j))
                .filter(i -> {
                    if (notBmkOnly) {
                        return i.getBookmarkData() != 1;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Illustration> search(Collection<String> keywords, Integer start, Integer end, String mode, boolean notBmkOnly, boolean searchTitle) {
        TaskProgress taskProgress = new TaskProgress("搜索" + Arrays.toString(keywords.toArray()));
        List<JSONObject> result = PixivPost.search(keywords, start, end, cookie, false, null, requestExecutor, taskProgress.getProgress());
        return result.stream()
                .map(j -> Illustration.parse((JSONObject) j))
                .filter(i -> {
                    if (notBmkOnly) {
                        return i.getBookmarkData() != 1;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }


    @Scheduled(cron = "0 3/10 * * * ?")
    @Override
    public void autoSearch() {
        IllustrationService illustrationService = SpringContextUtil.getBean(IllustrationService.class);
        FileService fileService = SpringContextUtil.getBean(FileService.class);

        keywordList = keywordList == null || keywordList.size() == 0 ? configService.getKeywordList() : keywordList;
        //随机序号的关键字
        int randomIndex = new Random().nextInt(keywordList.size());

        Config config = keywordList.get(randomIndex);
        String name = config.getName();
        log.info("自动搜索： {}", name);
        List<Callable<List<Illustration>>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int finalI = i + 1;
            tasks.add(() -> search(config.getValue(), finalI, null, true, false));
        }
        List<List<Illustration>> result = TasksUtil.executeTasks(tasks, 60, requestExecutor, "搜索", 4);

        List<String> idList = result.stream().flatMap(Collection::stream).map(Illustration::getId).collect(Collectors.toList());

        List<String> existIdList = illustrationService.findExistIdList(idList);

        idList.removeAll(existIdList);

        List<Illustration> details = illustrationService.findList(idList, 200);

        fileService.download(details, "搜索/" + name);

        keywordList.remove(randomIndex);
    }
}
