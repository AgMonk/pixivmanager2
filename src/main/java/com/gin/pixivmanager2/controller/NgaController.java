package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.NgaId;
import com.gin.pixivmanager2.service.ConfigService;
import com.gin.pixivmanager2.service.NgaService;
import com.gin.pixivmanager2.util.NgaPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;

/**
 * @author bx002
 */
@RestController
@RequestMapping("nga")
@Validated
@Slf4j
public class NgaController {
    private final ConfigService configService;
    private final NgaService ngaService;

    public NgaController(ConfigService configService, NgaService ngaService) {
        this.configService = configService;
        this.ngaService = ngaService;
    }

    @RequestMapping("getTidList")
    public List<NgaId> getTidList() {
        return configService.getTids();
    }

    @RequestMapping("getFidList")
    public List<NgaId> getFidList() {
        return configService.getFids();
    }

    @RequestMapping("getUser")
    public List<String> getUser() {
        return ngaService.getUser();
    }

    @RequestMapping("repost")
    public String repost(@NotEmpty String pid,
                         @NotEmpty String fid,
                         @NotEmpty String username,
                         @RequestParam(defaultValue = NgaPost.ACTION_REPLY) String action,
                         String tid) {
        List<String> pidCollection = Arrays.asList(pid.split(","));
        log.info("转发文件 {}", pidCollection);
        return ngaService.repost(pidCollection, fid, action, tid, username);
    }

}
