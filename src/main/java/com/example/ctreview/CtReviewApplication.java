package com.example.ctreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
@EnableScheduling
public class CtReviewApplication {

	public static void main(String[] args) {
		SpringApplication.run(CtReviewApplication.class, args);
	}
	@Bean
	public Clock systemClock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}

}
