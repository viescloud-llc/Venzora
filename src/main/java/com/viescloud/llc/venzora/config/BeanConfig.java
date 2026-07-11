package com.viescloud.llc.venzora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import com.viescloud.eco.viesspringutils.auto.config.ViesApplicationConfig;
import com.viescloud.eco.viesspringutils.auto.model.authentication.ViesDefaultEndpointEnum;
import com.viescloud.eco.viesspringutils.config.ViesMinimalBeanConfig;
import com.viescloud.eco.viesspringutils.exception.ExceptionAdvice;

@Configuration
@EnableScheduling
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
        config.setEnabledCheckoutOrderController(true);
        config.setEnabledCheckoutWebhookController(true);
        config.setEnabledCheckoutSubscriptionController(false);
        config.setEnabledCheckoutPlanController(false);
        return config;
    }
}
