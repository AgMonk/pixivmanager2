package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Tag;

import java.util.List;

public interface TagService {
    List<Tag> count(Integer offset, String keyword, Integer limit, boolean all);

    /**
     * 设置翻译
     *
     * @param tag
     */
    void setTranslation(Tag tag);
}
