package ru.majordomo.hms.rc.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import ru.majordomo.hms.rc.user.resources.WebSite;

@SpringBootApplication
@EnableEurekaClient
public class UsersResourceControllerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsersResourceControllerApplication.class, args);
	}
}
