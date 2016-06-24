package com.example.consumers;

import com.example.Route;

import java.util.Map;

public class Replay extends AbstractLogConsumer {

  public void receive(Map<String,String> record) {
    Route route = new Route(record);
    String path = route.get("path");

    if (null == System.getenv("REPLAY_HOST")) {
      System.out.println("Simulating request: " + path);
    } else {
      // TODO
    }
  }

}
