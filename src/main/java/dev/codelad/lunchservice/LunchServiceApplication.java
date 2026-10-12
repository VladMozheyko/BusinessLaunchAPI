package dev.codelad.lunchservice;

import dev.codelad.lunchservice.util.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class LunchServiceApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(LunchServiceApplication.class, args);
    }
}
