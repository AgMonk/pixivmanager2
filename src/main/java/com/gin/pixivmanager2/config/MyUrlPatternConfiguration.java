package com.gin.pixivmanager2.config;

import com.gin.pixivmanager2.service.ConfigService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author bx002
 */
@Configuration
public class MyUrlPatternConfiguration implements WebMvcConfigurer {
    private final String rootPath;

    public MyUrlPatternConfiguration(ConfigService configService) {
        this.rootPath = configService.getPath("rootPath").getValue();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //硬盘文件目录
        registry.addResourceHandler("/img/**").addResourceLocations("file:" + rootPath + "/");

        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }
}
