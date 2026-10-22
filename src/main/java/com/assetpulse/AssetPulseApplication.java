package com.assetpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AssetPulseApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetPulseApplication.class, args);
    }
}
