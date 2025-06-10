package com.viescloud.llc.venzora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.viescloud.eco.viesspringutils.auto.config.ViesBeanConfig;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass=true)
@Import(ViesBeanConfig.class)
@EnableJpaRepositories
@EntityScan
public class VenzoraApplication {

	public static void main(String[] args) {
		SpringApplication.run(VenzoraApplication.class, args);
	}
}
