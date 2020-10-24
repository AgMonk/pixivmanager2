package com.gin.pixivmanager2.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gin.pixivmanager2.entity.DownloadingFile;
import org.springframework.stereotype.Repository;

@Repository
public interface DownloadingFileDAO extends BaseMapper<DownloadingFile> {
}
