package com.example.common;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

/**
 * 跨域资源共享(CORS)配置类
 * 用于解决前后端分离架构中常见的跨域访问问题
 * 当前端应用与后端API不在同一域名下时，浏览器会出于安全考虑限制跨域请求
 * 本配置通过Spring提供的CORS支持解除这些限制
 */
@Configuration // 标识此类为Spring配置类，会被Spring容器扫描并加载
public class CorsConfig {

  /**
   * 创建CORS过滤器Bean，用于处理跨域请求
   * @return CorsFilter 跨域过滤器实例
   */
  @Bean // 将方法返回的对象注册为Spring容器中的Bean
  public CorsFilter corsFilter() {
    // 创建基于URL的CORS配置源，用于为不同URL路径配置不同的跨域规则
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // 创建CORS配置对象，存储跨域相关的配置信息
    CorsConfiguration corsConfiguration = new CorsConfiguration();

    // 允许所有来源的跨域请求
    // 注意：在生产环境中，建议指定具体的允许来源，而不是使用"*"
    corsConfiguration.addAllowedOrigin("*");

    // 允许所有请求头
    // 包含前端发送的各种自定义头信息，如Authorization等
    corsConfiguration.addAllowedHeader("*");

    // 允许所有HTTP请求方法
    // 包括GET、POST、PUT、DELETE、OPTIONS等
    corsConfiguration.addAllowedMethod("*");

    // 为所有路径注册CORS配置
    // "/**"表示匹配所有URL路径，应用上述的跨域配置
    source.registerCorsConfiguration("/**", corsConfiguration);

    // 创建并返回CORS过滤器实例，传入配置源
    return new CorsFilter(source);
  }
}
