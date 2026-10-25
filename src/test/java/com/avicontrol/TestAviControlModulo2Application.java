package com.avicontrol;

import org.springframework.boot.SpringApplication;

public class TestAviControlModulo2Application {

  public static void main(String[] args) {
    SpringApplication.from(AviControlModulo2Application::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
