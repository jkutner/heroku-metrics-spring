package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

  private static final Pattern MSG_PATTERN = Pattern.compile("(\\w+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");

  private String timestamp;

  private Map<String,String> message;

  private String rawMessage;

  public Route(Map<String, ?> log) {
    this.timestamp = log.get("syslog_TIMESTAMP").toString();

    this.message = new HashMap<>();

    rawMessage = log.get("syslog_MESSAGE").toString();
    Matcher m = MSG_PATTERN.matcher(rawMessage);

    while(m.find()) {
      message.put(m.group(1), m.group(2));
    }
  }

  public String get(String key) {
    return message.get(key);
  }

  public String timestamp() {
    return this.timestamp;
  }

  public String toString() {
    return rawMessage;
  }
}
