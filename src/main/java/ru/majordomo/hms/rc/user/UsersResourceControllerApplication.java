package ru.majordomo.hms.rc.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients(basePackages = {"ru.majordomo.hms.rc.user.api.interfaces"})
public class UsersResourceControllerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsersResourceControllerApplication.class, args);
	}
}
