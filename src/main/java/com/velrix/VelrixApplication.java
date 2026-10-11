package com.velrix;

import com.velrix.platform.infrastructure.config.VelrixAuthProperties;
import com.velrix.platform.infrastructure.config.VelrixJwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({VelrixAuthProperties.class, VelrixJwtProperties.class})
public class VelrixApplication {

	public static void main(String[] args) {
		SpringApplication.run(VelrixApplication.class, args);
	}

}
