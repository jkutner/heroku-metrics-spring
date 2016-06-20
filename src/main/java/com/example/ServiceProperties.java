package com.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "service", ignoreUnknownFields = false)
@Component
public class ServiceProperties {

  private String name = "World";

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

}