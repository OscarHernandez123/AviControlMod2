package com.avicontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AviControlModulo2Application {

  public static void main(String[] args) {
    SpringApplication.run(AviControlModulo2Application.class, args);
  }
}
