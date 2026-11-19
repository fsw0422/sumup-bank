package com.sumup.bank;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app on the host against a Testcontainers PostgreSQL, for IDE debugging.
 * Start with {@code ./gradlew bootTestRun} or run this class from the IDE.
 */
public class TestSumupBankApplication {

  public static void main(String[] args) {
    SpringApplication.from(SumupBankApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
