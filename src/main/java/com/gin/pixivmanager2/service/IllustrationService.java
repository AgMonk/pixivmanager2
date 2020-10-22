package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Illustration;

import java.util.Collection;
import java.util.List;

/**
 * @author bx002
 */
public interface IllustrationService {
    /**
     * 根据id查询一个作品
     * @param id
     * @return
     */
    Illustration findOneById(String id);

    /**
     * 根据多个id查询作品
     * @param idIn
     * @return
     */
    List<Illustration> findIDin(Collection<String> idIn);

    /**
     * 保存一个作品
     * @param collection
     * @return
     */
    Integer save(Collection<Illustration> collection);


}
