package org.example.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * REST 客户端配置
 * 用于声明 RestTemplate Bean，避免循环依赖问题
 */
@Configuration
public class RestClientConfig {

    /**
     * 创建 RestTemplate Bean，用于调用 Server 的 REST API
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
