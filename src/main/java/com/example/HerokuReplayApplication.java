package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HerokuReplayApplication implements HealthIndicator {

  @Override
  public Health health() {
    return Health.up().withDetail("hello", "world").build();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HerokuReplayApplication.class, args);
  }

}