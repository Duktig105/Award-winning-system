package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot应用程序启动类
 * 系统的主入口点，负责启动整个Spring Boot应用
 */
@SpringBootApplication  // 标识这是一个Spring Boot应用，包含自动配置、组件扫描等功能
@MapperScan("com.example.mapper")  // 扫描指定包下的Mapper接口，自动注册为MyBatis的Mapper
public class SpringbootBackendApplication {

	/**
	 * 应用程序主入口方法
	 * 启动Spring Boot应用程序
	 * @param args 命令行参数
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringbootBackendApplication.class, args);
	}

}
