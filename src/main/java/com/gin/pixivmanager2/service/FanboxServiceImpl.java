package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.FanboxItemDAO;
import com.gin.pixivmanager2.entity.FanboxItem;
import com.gin.pixivmanager2.util.FanboxPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@Slf4j
@Service
public class FanboxServiceImpl extends ServiceImpl<FanboxItemDAO, FanboxItem> implements FanboxService {
    private final String cookie;
    private final FileService fileService;

    public FanboxServiceImpl(ConfigService configService, FileService fileService) {
        this.cookie = configService.getCookie("fanbox").getValue();
        this.fileService = fileService;
    }

    @Override
    public List<FanboxItem> listCreator(String creatorId) {
        log.info("请求Fanbox用户作品: {}", creatorId);
        QueryWrapper<FanboxItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("creatorId", creatorId);
        List<FanboxItem> listFromDatabase = list(queryWrapper);
        List<String> idList = listFromDatabase.stream().map(FanboxItem::getId).collect(Collectors.toList());

        List<FanboxItem> listFromFanbox = new ArrayList<>();
        JSONArray array = FanboxPost.listCreator(creatorId, cookie);
        for (int i = 0; i < array.size(); i++) {
            listFromFanbox.add(new FanboxItem(array.getJSONObject(i)));
        }

        List<FanboxItem> newItemList = listFromFanbox.stream().filter(i -> !idList.contains(i.getId())).collect(Collectors.toList());
        if (newItemList.size() > 0) {
            log.info("发现新作品 {} 个", newItemList.size());
            newItemList.forEach(i -> {
                log.info("{} {} {}", i.getId(), i.getTitle(), i.getTime());
            });
            saveBatch(newItemList);
            fileService.download(newItemList);
        }

        return listFromFanbox;
    }

    @Override

    public List<FanboxItem> listSupporting(Integer limit) {
        List<FanboxItem> allItems = list();

        List<FanboxItem> listFromFanbox = new ArrayList<>();

        JSONArray array = FanboxPost.listSupporting(cookie, limit);
        for (int i = 0; i < array.size(); i++) {
            listFromFanbox.add(new FanboxItem(array.getJSONObject(i)));
        }

        listFromFanbox.removeIf(allItems::contains);
        log.info("Fanbox请求到 {} 件作品 其中新作品 {} 件", array.size(), listFromFanbox.size());
        if (listFromFanbox.size() > 0) {
            saveBatch(listFromFanbox);
            fileService.download(listFromFanbox);
        }
        return listFromFanbox;
    }


    @Override
    public void downloadItem(String postId) {
        FanboxItemDAO fanboxItemDAO = getBaseMapper();
        FanboxItem fanboxItem = fanboxItemDAO.selectById(postId);
        fanboxItem = fanboxItem != null ? fanboxItem : new FanboxItem(FanboxPost.postId(postId, cookie));
        saveOrUpdate(fanboxItem);
        fileService.download(fanboxItem);
    }
}
