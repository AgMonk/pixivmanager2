package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.IllustrationDAO;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.TaskProgress;
import com.gin.pixivmanager2.util.PixivPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@Slf4j
@Transactional
@Service
public class IllustrationServiceImpl extends ServiceImpl<IllustrationDAO, Illustration> implements IllustrationService {
    private final ThreadPoolTaskExecutor requestExecutor;
    private final ProgressService progressService;

    /**
     * 作品详情缓存
     */
    private final Map<String, Illustration> illustrationMap = new HashMap<>();

    public IllustrationServiceImpl(ThreadPoolTaskExecutor requestExecutor, ProgressService progressService) {
        this.requestExecutor = requestExecutor;
        this.progressService = progressService;
    }

    /**
     * 根据id查询一个作品
     *
     * @param id
     * @return
     */
    @Override
    public Illustration find(String id) {
        Illustration ill = illustrationMap.get(id);
        if (ill == null) {
            ill = getById(id);
        }
        long l = 30L * 24 * 60 * 60 * 1000;
        if (needUpdate(ill, 0)) {
            JSONObject detail = PixivPost.detail(id, null);
            if (detail != null) {
                ill = Illustration.parse(detail);
                saveOrUpdate(ill);
            }
        }
        illustrationMap.put(id, ill);

        return ill;
    }

    /**
     * 根据多个id查询作品
     *
     * @param ids
     * @param minBookCount
     * @return
     */
    @Override
    public List<Illustration> findList(Collection<String> ids, Integer minBookCount) {
        Map<String, Illustration> map = new HashMap<>(ids.size());
        List<String> lackList = new ArrayList<>(ids);

        //缓存
        illustrationMap.keySet().stream()
                .filter(lackList::contains)
                .forEach(s -> {
                    lackList.remove(s);
                    map.put(s, illustrationMap.get(s));
                });

        //查询数据库
        if (lackList.size() > 0) {
            QueryWrapper<Illustration> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", lackList);
            List<Illustration> daoList = list(queryWrapper);
            daoList.forEach(i -> {
                map.put(i.getId(), i);
                illustrationMap.put(i.getId(), i);
                lackList.remove(i.getId());
            });
        }

        //pixiv请求更新旧数据
        List<String> needPost = map.values().stream().filter(i -> needUpdate(i, minBookCount)).map(Illustration::getId).collect(Collectors.toList());
        needPost.addAll(lackList);
        log.info("数据库中有 {} 条数据 需要请求 {} 条数据", map.size(), needPost.size());
        TaskProgress detailProgress = progressService.add("详情");
        if (needPost.size() > 0) {
            List<Illustration> list = PixivPost.detail(needPost, null, requestExecutor, detailProgress.getProgress()).stream()
                    .map(Illustration::parse)
                    .filter(i -> i.getBookmarkCount() > minBookCount)
                    .peek(i -> {
                        map.put(i.getId(), i);
                        illustrationMap.put(i.getId(), i);
                    }).collect(Collectors.toList());
            progressService.remove(detailProgress);
            saveOrUpdateBatch(list);
        }
        

        return new ArrayList<>(map.values());
    }


    /**
     * 作品是否需要通过请求更新数据
     *
     * @param ill
     * @param minBookCount
     * @return
     */
    private static boolean needUpdate(Illustration ill, Integer minBookCount) {
        long l = 30L * 24 * 60 * 60 * 1000;
        return ill == null
                || ill.getLastUpdate() == null
                || System.currentTimeMillis() - ill.getLastUpdate() > l
                || ill.getBookmarkCount() < minBookCount;
    }
}
