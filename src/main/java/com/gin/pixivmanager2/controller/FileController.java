package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.response.Res;
import com.gin.pixivmanager2.service.ConfigService;
import com.gin.pixivmanager2.service.FileService;
import com.gin.pixivmanager2.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author bx002
 */
@RestController
@RequestMapping("file")
@Validated
@Slf4j
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @RequestMapping("del")
    public Res<Void> del(@NotEmpty String pid, @RequestParam(defaultValue = "未分类") String type) {
        log.info("删除: {}", pid);
        fileService.del(Arrays.asList(pid.split(",")), type);
        return new Res<>(200, "删除成功", null);
    }

    @RequestMapping("archive")
    public Res<Void> archive(@NotEmpty String pid, @RequestParam(defaultValue = "未分类") String type) {
        log.info("归档: {}", pid);
        fileService.archive(Arrays.asList(pid.split(",")), type);
        return new Res<>(200, "归档成功", null);
    }

    @RequestMapping("getUntagged")
    public Map<String, String> getUntagged() {
        Map<String, File> untagged = fileService.getFileMap("未分类");
        String rootPath = SpringContextUtil.getBean(ConfigService.class).getPath("rootPath").getValue();
        TreeMap<String, String> map = new TreeMap<>((s1, s2) -> {
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

        while (map.size() > 50) {
            map.pollLastEntry();
        }

        return map;
    }
}
