package com.musicplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置。
 * 启动后访问 http://localhost:8080/swagger-ui.html 查看接口文档。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI musicPlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("音乐创作智能体平台 API")
                        .version("1.0.0")
                        .description("音乐创作智能体平台后端接口文档 — 成员B")
                        .contact(new Contact()
                                .name("成员B")
                                .email("b@music-platform.dev")));
    }
}
