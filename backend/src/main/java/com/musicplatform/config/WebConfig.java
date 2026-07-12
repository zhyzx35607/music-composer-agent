package com.musicplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web 配置 — 静态文件访问映射。
 * 将 /outputs/** 路径映射到本地 outputs 目录，
 * 前端可以直接访问 MIDI 和 WAV 文件。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${music-platform.output-dir:./outputs}")
    private String outputDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /outputs/** URL 映射到本地 outputs/ 目录（转为绝对路径避免相对路径问题）
        String absolutePath = Path.of(outputDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/outputs/**")
                .addResourceLocations(absolutePath);
    }
}
