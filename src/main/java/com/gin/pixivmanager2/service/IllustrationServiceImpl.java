package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.IllustrationDAO;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.util.PixivPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@Slf4j
@Transactional
@Service
public class IllustrationServiceImpl extends ServiceImpl<IllustrationDAO, Illustration> implements IllustrationService {
    private final ThreadPoolTaskExecutor requestExecutor;

    /**
     * 作品详情缓存
     */
    private final Map<String, Illustration> illustrationMap = new HashMap<>();

    public IllustrationServiceImpl(ThreadPoolTaskExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;


    }

    /**
     * 根据id查询一个作品
     *
     * @param id
     * @return
     */
    @Override
    public Illustration findOneById(String id) {
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
        //查询缓存
        List<Illustration> cachedList =
                illustrationMap.keySet().stream()
                        .filter(ids::contains)
                        .map(illustrationMap::get)
                        .filter(i -> !needUpdate(i, minBookCount))
                        .collect(Collectors.toList());
        List<String> cachedIds = cachedList.stream().map(Illustration::getId).collect(Collectors.toList());

        List<String> lackList = ids.stream().filter(id -> !cachedIds.contains(id)).collect(Collectors.toList());
        if (lackList.size() > 0) {
            //查询数据库
            QueryWrapper<Illustration> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", lackList);
            List<Illustration> daoList = list(queryWrapper);
            //放入缓存
            daoList.stream().filter(i -> !needUpdate(i, minBookCount)).forEach(i -> {
                illustrationMap.put(i.getId(), i);
                cachedList.add(i);
                lackList.remove(i.getId());
            });
        }
        if (lackList.size() > 0) {
            List<Illustration> list = PixivPost.detail(lackList, null, requestExecutor, new HashMap<>()).stream()
                    .map(Illustration::parse)
                    .filter(i -> i.getBookmarkCount() > minBookCount)
                    .collect(Collectors.toList());
            cachedList.addAll(list);
            list.forEach(i -> illustrationMap.put(i.getId(), i));
            saveOrUpdateBatch(list);
        }


        return cachedList;
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
        return ill == null || System.currentTimeMillis() - ill.getLastUpdate() > l || ill.getBookmarkCount() < minBookCount;
    }
}
