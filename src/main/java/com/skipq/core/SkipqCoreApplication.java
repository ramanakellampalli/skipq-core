package com.skipq.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkipqCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkipqCoreApplication.class, args);
	}

}
