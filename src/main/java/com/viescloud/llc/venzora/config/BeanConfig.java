package com.viescloud.llc.venzora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.viescloud.eco.viesspringutils.config.ViesMinimalBeanConfig;

@Configuration
public class BeanConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        return ViesMinimalBeanConfig.defaultCorsWebFilter();
    }

    @Bean
    public WebMvcConfigurer CORSConfigurer() {
        return ViesMinimalBeanConfig.defaultCORSConfigurer();
    }
}
