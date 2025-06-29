package co.estimoo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlanningVoteApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanningVoteApplication.class, args);
	}

}
