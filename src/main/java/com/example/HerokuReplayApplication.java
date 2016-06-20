package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.messaging.MessageHandler;

import static java.lang.String.format;

@SpringBootApplication
public class HerokuReplayApplication {

  @ServiceActivator(inputChannel = "toKafka")
  @Bean
  public MessageHandler kafkaHandler() throws Exception {
    KafkaProducerMessageHandler<String, String> handler =
        new KafkaProducerMessageHandler<>(kafkaTemplate());
    handler.setTopicExpression(new LiteralExpression(KafkaConfig.getTopic()));
    handler.setMessageKeyExpression(new LiteralExpression(KafkaConfig.getMessageKey()));
    return handler;
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public ProducerFactory<String, String> producerFactory() {
    return new DefaultKafkaProducerFactory<>(KafkaConfig.producerDefaults());
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HerokuReplayApplication.class, args);
  }

}