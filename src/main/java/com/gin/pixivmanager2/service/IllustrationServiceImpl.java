package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.IllustrationDAO;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.TaskProgress;
import com.gin.pixivmanager2.util.PixivPost;
import com.gin.pixivmanager2.util.Request;
import com.gin.pixivmanager2.util.TasksUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.gin.pixivmanager2.util.ListUtils.spiltList;

/**
 * @author bx002
 */
@Slf4j
@Service
public class IllustrationServiceImpl extends ServiceImpl<IllustrationDAO, Illustration> implements IllustrationService {
    private final ThreadPoolTaskExecutor requestExecutor;
    private final ProgressService progressService;
    private final static long MS_OF_DAY = 24L * 60 * 60 * 1000;
    private final static int DAY_LIMITS = 45;

    /**
     * 作品详情缓存
     */

    public IllustrationServiceImpl(ThreadPoolTaskExecutor requestExecutor, ProgressService progressService) {
        this.requestExecutor = requestExecutor;
        this.progressService = progressService;
    }

    /**
     * 查询数据库中已存在 且较新的详情数据
     *
     * @param idCollection
     * @return java.util.List<java.lang.String>
     * @author bx002
     * @date 2020/11/23 11:39
     */
    @Override
    public List<String> findExistIdList(Collection<String> idCollection) {
        QueryWrapper<Illustration> qw = new QueryWrapper<>();
        qw.select("id")
                .in("id", idCollection)
                .isNotNull("lastUpdate")
                .ge("lastUpdate", getLimit());
        return list(qw).stream().map(Illustration::getId).collect(Collectors.toList());
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
        //查询数据库中 在id集合中 但 不需要更新的详情数据
        List<String> lackList = new ArrayList<>(ids);
        List<String> noNeedUpdate = findExistIdList(lackList);
        lackList.removeAll(noNeedUpdate);

        if (lackList.size() > 0) {
            log.info("需要请求 {} 条数据", lackList.size());
            getDetail(lackList, minBookCount);
        }

        QueryWrapper<Illustration> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        return list(queryWrapper);
    }

    /**
     * 不带条件请求并更新作品详情
     *
     * @param id
     * @param minBookCount 只记录收藏数大于等于该数的数据
     * @return
     */
    @Override
    public Illustration getDetail(String id, Integer minBookCount) {
        JSONObject detail = PixivPost.detail(id, null);
        //从pixiv成功查询到作品详情 保存/更新详情
        if (detail != null) {
            Illustration ill = Illustration.parse(detail);
            if (minBookCount == null || ill.getBookmarkCount() >= minBookCount) {
                saveOrUpdate(ill);
            }
            return ill;
        }
        //查询作品详情失败 设置更新时间 不再更新
        Illustration byId = getById(id);
        if (byId != null) {
            Illustration entity = new Illustration();
            long noLongerUpdate = 9999999999999L;
            entity.setId(byId.getId()).setLastUpdate(noLongerUpdate);
            updateById(entity);
            return byId.setLastUpdate(noLongerUpdate);
        }
        return new Illustration();
    }

    /**
     * 请求详情并更新数据库
     *
     * @param needPost
     * @param minBookCount
     * @return java.util.List<com.gin.pixivmanager2.entity.Illustration>
     * @author bx002
     * @date 2020/11/14 10:34
     */
    @Override
    public List<Illustration> getDetail(Collection<String> needPost, Integer minBookCount) {
        long start = System.currentTimeMillis();
        ArrayList<String> list = new ArrayList<>(needPost);

        spiltList(list, 5).forEach(l -> log.info("[请求详情] {}", l));

        TaskProgress detailProgress = progressService.add("详情任务", list.size());
        List<Callable<Illustration>> tasks = new ArrayList<>();
        needPost.forEach(id -> {
            tasks.add(() -> {
                Illustration detail = getDetail(id, minBookCount);
                detailProgress.addCount(1);
                return detail;
            });
        });

        List<Illustration> detail = TasksUtil
                .executeTasks(tasks, 50, requestExecutor, "detail", 2)
                .stream().filter(ill -> ill.getId() != null).collect(Collectors.toList());
        ;

        long end = System.currentTimeMillis();
        log.info("获得作品详情 {} 个 耗时 {}", detail.size(), Request.timeCost(start, end));
        progressService.remove(detailProgress);
        return detail;
    }

    @Override
    public void update(@DefaultValue("10") Integer step) {

        QueryWrapper<Illustration> queryWrapper = new QueryWrapper<>();

        queryWrapper
                .isNull("lastUpdate")
                .or().le("lastUpdate", getLimit())
        ;
        int count = count(queryWrapper);

        queryWrapper
                .select("id")
                .orderByDesc("id")
                .last("limit 0," + step)
        ;
        List<String> idList = list(queryWrapper).stream().map(Illustration::getId).collect(Collectors.toList());

        int size = idList.size();
        log.info("[更新详情] 有 {} 条详情待更新  本次更新 {} 条 需要更新 {} 次", count, size, count / size);

        findList(idList, 0);

    }

        @Scheduled(cron = "0 * * * * ?")
    void autoUpdate() {
        update(10);
    }


    private static long getLimit() {
        return System.currentTimeMillis() - DAY_LIMITS * MS_OF_DAY;
    }
}
