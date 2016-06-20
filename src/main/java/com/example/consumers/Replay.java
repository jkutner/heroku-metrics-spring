package com.example.consumers;

import com.example.KafkaConfig;
import com.example.Route;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;

public class Replay {

  private ExecutorService executor;

  private KafkaConsumer<String, String> consumer;

  private final AtomicBoolean running = new AtomicBoolean();

  private CountDownLatch stopLatch;

  private ObjectMapper mapper = new ObjectMapper();

  private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

  public static void main(String[] args) {
    Replay replay = new Replay();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        replay.stop();
      }
    });

    replay.start();
  }

  public void start() {
    running.set(true);
    executor = Executors.newSingleThreadExecutor();
    executor.submit(this::loop);
    stopLatch = new CountDownLatch(1);
  }

  public void stop() {
    running.set(false);
    try {
      stopLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      executor.shutdown();
    }
  }

  private void loop() {
    System.out.println("starting consumer...");
    Properties properties = new Properties();
    properties.putAll(KafkaConfig.consumerDefaults());

    consumer = new KafkaConsumer<>(properties);
    consumer.subscribe(singletonList(KafkaConfig.getTopic()));

    do {
      ConsumerRecords<String, String> records = consumer.poll(100);
      for (ConsumerRecord<String, String> record : records) {
        try {
          String path = parseRoute(record);
          if (null == System.getenv("REPLAY_HOST")) {
            System.out.println("Simulating request: " + path);
          } else {
            // TODO
          }
        } catch (IOException e) {
          System.out.println("Skipping route: " + record.value());
          e.printStackTrace();
        }
      }
    } while (running.get());

    System.out.println("closing consumer...");
    consumer.close();
    stopLatch.countDown();
  }

  private String parseRoute(ConsumerRecord<String, String> record) throws IOException {
    Map<String,String> recordMap = mapper.readValue(record.value(), typeRef);
    Route route = new Route(recordMap);
    return route.get("path");
  }
}
