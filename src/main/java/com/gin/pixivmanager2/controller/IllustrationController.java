package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.service.ConfigService;
import com.gin.pixivmanager2.service.IllustrationService;
import com.gin.pixivmanager2.util.NgaPost;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
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

    @RequestMapping(value = "find")
    public Illustration find(@NotEmpty @DecimalMin(value = "1") String id) {
        return illustrationService.find(id);
    }

    @RequestMapping(value = "findList")
    public List<Illustration> findList(String idString) {
        return illustrationService.findList(Arrays.asList(idString.split(",")), 200, false);
    }

    @RequestMapping("test")
    public Object test(String idString) {

        String cookie = configService.getCookie("nga").getValue();

        String url = NgaPost.create(cookie, "-993945", "23293528", NgaPost.ACTION_REPLY)
                .addContent("测试测试测试").send();


        return null;
    }


}
