package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.DownloadingFile;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.TwitterImage;
import com.gin.pixivmanager2.entity.response.Res;
import com.gin.pixivmanager2.service.ConfigService;
import com.gin.pixivmanager2.service.FileService;
import com.gin.pixivmanager2.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

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
        fileService.archivePixiv(Arrays.asList(pid.split(",")), type);
        return new Res<>(200, "归档成功", null);
    }


    @RequestMapping("addRepostQueue")
    public Res<Void> addRepostQueue(@NotEmpty String pid, @RequestParam(defaultValue = "未分类") String type) {
        log.info("添加转发队列: {}", pid);
        fileService.addRepostQueue(Arrays.asList(pid.split(",")), type);
        return new Res<>(200, "转发队列添加成功", null);
    }

    public Res<Void> repost(@NotEmpty String pid) {
        log.info("转发: {}", pid);

        return null;
    }

    @RequestMapping("archiveOld")
    public Res<Void> archiveOld() {
        log.info("归档旧作品");
        fileService.archivePixiv(null, "old");
        return new Res<>(200, "归档成功", null);
    }

//    @Scheduled(cron = "30 0/5 * * * ?")
    public void autoArchiveOld() {
        archiveOld();
    }

    @RequestMapping("getFileMap")
    public Map<String, String> getFileMap(@RequestParam(defaultValue = "未分类") String type,
                                          @RequestParam(defaultValue = "50") Integer limit) {
        Map<String, File> untagged = fileService.getFileMap(type);
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

        while (map.size() > limit) {
            map.pollLastEntry();
        }

        return map;
    }

    @RequestMapping("getTwitterImages")
    public List<TwitterImage> getTwitterImages() {
        ArrayList<TwitterImage> list = new ArrayList<>();
        Map<String, String> twitter = getFileMap("twitter", 50);
        twitter.forEach((k, v) -> {
            TwitterImage image = TwitterImage.parse(v);
            String tags = image.getTags();
            if (tags != null) {
                String[] split = tags.split(",");
                Map<String, String> dic = Illustration.dic;
                for (int i = 0; i < split.length; i++) {
                    String key = split[i].toLowerCase();
                    if (dic.containsKey(key)) {
                        split[i] = dic.get(key);
                    }
                }
                image.setTags(Arrays.stream(split).distinct().collect(Collectors.joining(",")));
            }

            list.add(image);
        });
        return list;
    }


    @RequestMapping("archiveTwitter")
    @ResponseBody
    public Res<Void> archiveTwitter(@RequestBody TwitterImage image) {
        fileService.archiveTwitter(image);
        return new Res<>(200, "归档成功", null);
    }

    @RequestMapping("getDownloadingFileList")
    public Res<List<DownloadingFile>> getDownloadingFileList() {
        return new Res<>(2000, "正在下载的文件列表 获取成功", fileService.getDownloadingFileList());
    }

    @RequestMapping("test")
    public void test() {
        fileService.download();
    }
}
