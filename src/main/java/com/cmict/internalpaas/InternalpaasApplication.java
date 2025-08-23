package com.cmict.internalpaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InternalpaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(InternalpaasApplication.class, args);
	}

}
