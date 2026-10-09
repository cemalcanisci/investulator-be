package com.investulator;

import org.springframework.boot.SpringApplication;

public class TestInvestulatorBeApplication {

  public static void main(String[] args) {
    SpringApplication.from(InvestulatorBeApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
