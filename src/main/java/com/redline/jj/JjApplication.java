package com.redline.jj;

import com.redline.jj.config.CrawlerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CrawlerProperties.class)
public class JjApplication {

    public static void main(String[] args) {
		SpringApplication.run(JjApplication.class, args);
	}

}
