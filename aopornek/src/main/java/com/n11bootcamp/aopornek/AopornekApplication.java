package com.n11bootcamp.aopornek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(proxyTargetClass=true)
@SpringBootApplication
public class AopornekApplication {

	public static void main(String[] args) {
		SpringApplication.run(AopornekApplication.class, args);
	}

}
