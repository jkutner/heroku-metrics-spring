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

public abstract class AbstractLogConsumer {

  public abstract void receive(Route route);

  private ExecutorService executor;

  private KafkaConsumer<String, String> consumer;

  private final AtomicBoolean running = new AtomicBoolean();

  private CountDownLatch stopLatch;

  private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

  private ObjectMapper mapper = new ObjectMapper();

  public void start() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        stopLoop();
      }
    });

    running.set(true);
    executor = Executors.newSingleThreadExecutor();
    executor.submit(this::loop);
    stopLatch = new CountDownLatch(1);
  }

  public void stopLoop() {
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
          Map<String,String> recordMap = mapper.readValue(record.value(), typeRef);
          Route route = new Route(recordMap);
          receive(route);
        } catch (IOException e) {
          System.out.println("Error parsing record: " + record.value());
          e.printStackTrace();
        }
      }
    } while (running.get());

    System.out.println("closing consumer...");
    consumer.close();
    stopLatch.countDown();
  }
}
