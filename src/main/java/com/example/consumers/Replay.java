package com.example.consumers;

import com.example.Route;

import javax.net.ssl.HttpsURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

public class Replay extends AbstractLogConsumer {

  public static void main(String[] args) {
    new Replay().start();
  }

  @Override
  public void receive(Route route) {
    if ("GET".equals(route.get("method"))) {
      String path = route.get("path");

      if (null == System.getenv("REPLAY_HOST")) {
        System.out.println("Simulating request: " + path);
      } else {
        try {
          URL url = new URL(new URL(System.getenv("REPLAY_HOST")), path);
          HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
          con.setRequestMethod("GET");
          int responseCode = con.getResponseCode();
          System.out.println("Sent request (" + responseCode + "): " + path);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

}
