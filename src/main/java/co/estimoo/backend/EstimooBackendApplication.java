package co.estimoo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EstimooBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EstimooBackendApplication.class, args);
    }

} 