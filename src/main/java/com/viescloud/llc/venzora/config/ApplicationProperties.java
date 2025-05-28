package com.viescloud.llc.venzora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class ApplicationProperties {

    @Value("${spring.profiles.active}")
    private String env;
}
