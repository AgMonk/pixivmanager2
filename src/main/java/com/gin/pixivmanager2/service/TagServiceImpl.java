package com.gin.pixivmanager2.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.TagDAO;
import com.gin.pixivmanager2.dao.TagFromIllustDAO;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.Tag;
import com.gin.pixivmanager2.entity.TagFromIllust;
import com.gin.pixivmanager2.util.PixivPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bx002
 */
@Slf4j
@Transactional
@Service
public class TagServiceImpl extends ServiceImpl<TagDAO, Tag> implements TagService {
    private final ThreadPoolTaskExecutor requestExecutor;
    private final TagFromIllustDAO tagFromIllustDAO;
    private final ThreadPoolTaskExecutor initExecutor;
    private final TagDAO tagDAO;

    private final String cookie;
    private final String tt;
    Map<String, Tag> tagMap;
    /**
     * 从作品详情中获取的tag及其原生翻译
     */
    List<TagFromIllust> tagFromIllusts;
    /**
     * 自定义tag翻译
     */
    private final Map<String, String> customTranslations = new HashMap<>();


    public TagServiceImpl(ThreadPoolTaskExecutor requestExecutor,
                          TagFromIllustDAO tagFromIllustDAO,
                          ThreadPoolTaskExecutor initExecutor,
                          TagDAO tagDAO,
                          ConfigService configService) {
        this.requestExecutor = requestExecutor;
        this.tagFromIllustDAO = tagFromIllustDAO;
        this.initExecutor = initExecutor;
        this.tagDAO = tagDAO;
        cookie = configService.getCookie("pixiv").getValue();
        tt = configService.getConfig("tt").getValue();

        initExecutor.execute(() -> {
            tagFromIllusts = tagFromIllustDAO.selectList(null);
            updateTagMap();
        });
        initExecutor.execute(() -> {
            tagDAO.selectList(null).forEach(t -> customTranslations.put(t.getName().toLowerCase(), t.getTranslation()));
            Tag.dic = customTranslations;
            Illustration.dic = customTranslations;
        });
    }

    @Override
    public List<Tag> count(Integer offset, String keyword, Integer limit, boolean all) {
        Stream<Tag> stream = tagMap.values().stream()
                .filter(tag -> tag.getName().toLowerCase().contains(keyword)
                        || tag.getTranslation().toLowerCase().contains(keyword));
        if (!all) {
            stream = stream.filter(t -> !customTranslations.containsKey(t.getName().toLowerCase()));
        }
        return stream
                .sorted((o1, o2) -> o2.getCount() - o1.getCount())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 设置翻译
     *
     * @param tag
     */
    @Override
    public void setTranslation(Tag tag) {
        saveOrUpdate(tag);
        tagMap.remove(tag.getName());
        requestExecutor.execute(() -> PixivPost.setTag(cookie, tt, tag.getName(), tag.getTranslation()));
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void updateTagMap() {
        tagMap = new HashMap<>();
        tagFromIllusts.forEach(t -> {
            t.getTagList().forEach(tt -> {
                Tag tag = tagMap.get(tt.getName());
                if (tag == null) {
                    tag = tt;
                    tagMap.put(tag.getName(), tag);
                }
                tag.add();
            });
        });
    }
}
