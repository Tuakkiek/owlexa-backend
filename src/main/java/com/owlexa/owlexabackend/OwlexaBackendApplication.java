package com.owlexa.owlexabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OwlexaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OwlexaBackendApplication.class, args);
    }

}
