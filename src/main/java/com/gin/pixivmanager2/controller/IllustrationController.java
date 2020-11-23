package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.service.ConfigService;
import com.gin.pixivmanager2.service.IllustrationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * @author bx002
 */
@RestController
@RequestMapping("ill")
@Validated
public class IllustrationController {

    private final IllustrationService illustrationService;
    private final ConfigService configService;


    public IllustrationController(IllustrationService illustrationService, ConfigService configService) {
        this.illustrationService = illustrationService;
        this.configService = configService;
    }


    @RequestMapping(value = "findList")
    public List<Illustration> findList(String idString) {
        return illustrationService.findList(Arrays.asList(idString.split(",")), 200);
    }

    @RequestMapping("test")
    public Object test(String idString) {

        illustrationService.autoUpdate();

        return null;
    }


}
