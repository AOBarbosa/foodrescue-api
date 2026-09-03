package br.com.seudominio.foodrescue.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "br.com.seudominio.foodrescue")
@EntityScan("br.com.seudominio.foodrescue.domain.entities")
@EnableJpaRepositories("br.com.seudominio.foodrescue.persistence.repositories")
public class FoodRescueApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodRescueApiApplication.class, args);
    }

}
