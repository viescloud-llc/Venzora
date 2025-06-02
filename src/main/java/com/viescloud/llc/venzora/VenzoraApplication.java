package com.viescloud.llc.venzora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import com.viescloud.eco.viesspringutils.ViesApplicationMinimal;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class VenzoraApplication extends ViesApplicationMinimal {

	public static void main(String[] args) {
		SpringApplication.run(VenzoraApplication.class, args);
	}

	@Override
	public String getApplicationName() {
		return "venzora";
	}

}
