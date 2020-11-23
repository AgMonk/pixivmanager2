package com.gin.pixivmanager2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gin.pixivmanager2.entity.Illustration;

import java.util.Collection;
import java.util.List;

/**
 * @author bx002
 */
public interface IllustrationService extends IService<Illustration> {

    List<String> findExistIdList(Collection<String> idCollection);

    /**
     * 根据多个id查询作品
     *
     * @param ids
     * @param minBookCount
     * @return
     */
    List<Illustration> findList(Collection<String> ids, Integer minBookCount);


    List<Illustration> getDetail(Collection<String> needPost, Integer minBookCount);

    void autoUpdate();
}
