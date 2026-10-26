package com.aicte.fdrportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FdrPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(FdrPortalApplication.class, args);
    }

}
