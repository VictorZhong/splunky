package com.wpb.spky;

import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.config.SplunkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SplunkyProperties.class, SplunkProperties.class})
public class SplunkyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplunkyServiceApplication.class, args);
    }
}
