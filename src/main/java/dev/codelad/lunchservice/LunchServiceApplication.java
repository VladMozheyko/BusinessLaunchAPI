package dev.codelad.lunchservice;

import dev.codelad.lunchservice.util.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class LunchServiceApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(LunchServiceApplication.class, args);
    }
}
