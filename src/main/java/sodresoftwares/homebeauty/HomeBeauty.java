package sodresoftwares.homebeauty;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@EnableAsync
@SpringBootApplication
public class HomeBeauty {

	public static void main(String[] args) {
		SpringApplication.run(HomeBeauty.class, args);
	}

	@PostConstruct
	public void init() {
		// Set the default time zone to UTC
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
}