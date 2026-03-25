package com.viescloud.llc.venzora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.viescloud.eco.viesspringutils.auto.config.ViesApplicationConfig;
import com.viescloud.eco.viesspringutils.auto.model.authentication.ViesDefaultEndpointEnum;
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

    @Bean
    public ViesApplicationConfig viesApplicationConfig(@Value("${spring.profiles.active:local}") String env) {
        var config = new ViesApplicationConfig(env, ViesDefaultEndpointEnum.toList());
        config.setEnabledHttpClientController(true);
        return config;
    }
}
