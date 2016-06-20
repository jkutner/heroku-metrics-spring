package com.example.consumers;

import com.example.kafka.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

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
        System.out.println("Got message: " + record.value());
      }
    } while (running.get());

    System.out.println("closing consumer...");
    consumer.close();
    stopLatch.countDown();
  }
}
