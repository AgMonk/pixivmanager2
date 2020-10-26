package com.gin.pixivmanager2.config;

import com.gin.pixivmanager2.service.ConfigService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class MyUrlPatternConfiguration extends WebMvcConfigurationSupport {
    private final ConfigService configService;
    private final String rootPath;

    public MyUrlPatternConfiguration(ConfigService configService) {
        this.configService = configService;
        this.rootPath = configService.getPath("rootPath").getValue();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //硬盘文件目录
        registry.addResourceHandler("/img/**").addResourceLocations("file:" + rootPath + "/");

        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        super.addResourceHandlers(registry);
    }
}
