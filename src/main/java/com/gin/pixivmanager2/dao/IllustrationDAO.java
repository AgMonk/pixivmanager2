package com.gin.pixivmanager2.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gin.pixivmanager2.entity.Illustration;
import org.apache.ibatis.annotations.CacheNamespace;
import org.springframework.stereotype.Repository;

/**
 * @author bx002
 */
@Repository
@CacheNamespace
public interface IllustrationDAO extends BaseMapper<Illustration> {

}
