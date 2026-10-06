package com.example.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.example.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Web 配置类
 * 用于配置静态资源映射，解决前端无法直接访问本机盘符绝对路径的问题。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login", "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目运行根目录
        String projectDir = System.getProperty("user.dir");
        // 将 /uploads/** 的网络请求映射到项目根目录下的 /uploads/ 文件夹中
        String staticMappingPath = "file:" + projectDir + "/uploads/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(staticMappingPath);
    }

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
