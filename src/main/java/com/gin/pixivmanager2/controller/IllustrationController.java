package com.gin.pixivmanager2.controller;

import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.entity.Tag;
import com.gin.pixivmanager2.service.IllustrationService;
import com.gin.pixivmanager2.service.TagServiceImpl;
import com.gin.pixivmanager2.util.SpringContextUtil;
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


    public IllustrationController(IllustrationService illustrationService) {
        this.illustrationService = illustrationService;
    }

    @RequestMapping(value = "findOneById")
    public Illustration findOneById(@NotEmpty @DecimalMin(value = "1") String id) {
        return illustrationService.findOneById(id);
    }

    @RequestMapping(value = "findListById")
    public List<Illustration> findListById(String idString) {
        return illustrationService.findList(Arrays.asList(idString.split(",")), 0);
    }

    @RequestMapping("test")
    public List<Tag> test() {
        TagServiceImpl bean = SpringContextUtil.getBean(TagServiceImpl.class);
        return bean.count(5, 10, false);
    }
}
