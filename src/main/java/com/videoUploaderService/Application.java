package com.videoUploaderService;

import com.videoUploaderService.config.DotenvPropertySourceInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
		
		SpringApplication app = new SpringApplication(Application.class);
		app.addInitializers(new DotenvPropertySourceInitializer());
		app.run(args);
	}

}
