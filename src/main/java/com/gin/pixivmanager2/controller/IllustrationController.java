package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.service.ConfigService;
import com.gin.pixivmanager2.service.FileServiceImpl;
import com.gin.pixivmanager2.service.IllustrationService;
import com.gin.pixivmanager2.util.SpringContextUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author bx002
 */
@RestController
@RequestMapping("ill")
@Validated
public class IllustrationController {

    private final IllustrationService illustrationService;


    public IllustrationController(IllustrationService illustrationService) {
        this.illustrationService = illustrationService;
    }

    @RequestMapping(value = "find")
    public Illustration find(@NotEmpty @DecimalMin(value = "1") String id) {
        return illustrationService.find(id);
    }

    @RequestMapping(value = "findList")
    public List<Illustration> findList(String idString) {
        return illustrationService.findList(Arrays.asList(idString.split(",")), 200);
    }

    @RequestMapping("test")
    public Object test(String idString) {
        return SpringContextUtil.getBean(FileServiceImpl.class)
                .getFileMap("未分类");

    }

    @RequestMapping("getUntagged")
    public Map<String, String> getUntagged() {
        Map<String, File> untagged = SpringContextUtil.getBean(FileServiceImpl.class).getFileMap("未分类");
        String rootPath = SpringContextUtil.getBean(ConfigService.class).getPath("rootPath").getValue();
        Map<String, String> map = new TreeMap<>((s1, s2) -> {
            if (!s1.contains("_p") || !s2.contains("_p")) {
                return s1.compareTo(s2);
            } else {
                String[] p1 = s1.split("_p");
                String[] p2 = s2.split("_p");

                if (p1[0].equals(p2[0])) {
                    return p1[1].compareTo(p2[1]);
                } else {
                    return -1 * p1[0].compareTo(p2[0]);
                }
            }
        });
        untagged.forEach((k, v) -> {
            String replace = "/img" + v.getPath().replace("\\", "/").replace(rootPath, "");
            map.put(k, replace);

        });

        return map;
    }
}
