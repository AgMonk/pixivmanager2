package com.gin.pixivmanager2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gin.pixivmanager2.entity.Illustration;

import java.util.Collection;
import java.util.List;

/**
 * @author bx002
 */
public interface IllustrationService extends IService<Illustration> {
    /**
     * 根据id查询一个作品
     *
     * @param id
     * @return
     */
    Illustration findOneById(String id);

    /**
     * 根据多个id查询作品
     *
     * @param ids
     * @return
     */
    List<Illustration> findList(Collection<String> ids);

    
}
