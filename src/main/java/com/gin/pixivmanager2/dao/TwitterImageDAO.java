package com.gin.pixivmanager2.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gin.pixivmanager2.entity.TwitterImage;
import org.springframework.stereotype.Repository;

/**
 * 推特图片DAO
 *
 * @author bx002
 * @date 2020/12/3 14:12
 */
@Repository
public interface TwitterImageDAO extends BaseMapper<TwitterImage> {
}
