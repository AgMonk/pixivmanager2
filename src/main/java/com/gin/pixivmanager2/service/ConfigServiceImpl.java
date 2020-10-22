package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.dao.ConfigDAO;
import com.gin.pixivmanager2.dao.NgaIDDAO;
import com.gin.pixivmanager2.entity.Config;
import com.gin.pixivmanager2.entity.NgaId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {
    private final ConfigDAO configDAO;
    private final NgaIDDAO  ngaIDDAO;

    private final List<Config> configList;
    private final List<NgaId> ngaIdList;

    public ConfigServiceImpl(ConfigDAO configDAO, NgaIDDAO ngaIDDAO) {
        this.configDAO = configDAO;
        this.ngaIDDAO = ngaIDDAO;

        configList = configDAO.selectList(null);
        ngaIdList = ngaIDDAO.selectList(null);

        configList.forEach(c->log.debug("载入配置[{}] {} -> {}",c.getType(),c.getName(),c.getValue().substring(0,Math.min(50,c.getValue().length()))));
        ngaIdList.forEach(n->log.debug("载入ngaId [{}] {} -> {}",n.getType(),n.getName(),n.getId()));
    }


    @Override
    public List<NgaId> getFids() {
        return ngaIdList.stream().filter(c -> "fid".equals(c.getType())).collect(Collectors.toList());
    }

    @Override
    public List<NgaId> getTids() {
        return ngaIdList.stream().filter(c -> "tid".equals(c.getType())).collect(Collectors.toList());
    }

    @Override
    public Config getCookie(String name) {
        return configList.stream().filter(c -> "cookie".equals(c.getType()) && name.equals(c.getName())).findFirst().orElse(null);
    }

    @Override
    public Config getPath(String name) {
        return configList.stream().filter(c -> "path".equals(c.getType()) && name.equals(c.getName())).findFirst().orElse(null);
    }

    @Override
    public Config getConfig(String name) {
        return configList.stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }
}
