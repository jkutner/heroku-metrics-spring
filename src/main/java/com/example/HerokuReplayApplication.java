package com.example;

import com.github.jkutner.EnvKeyStore;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@SpringBootApplication
public class HerokuReplayApplication {

  @ServiceActivator(inputChannel = "toKafka")
  @Bean
  public MessageHandler kafkaHandler() throws Exception {
    KafkaProducerMessageHandler<String, String> handler =
        new KafkaProducerMessageHandler<>(kafkaTemplate());
//    handler.setTopicExpression(new LiteralExpression(this.topic));
//    handler.setMessageKeyExpression(new LiteralExpression(this.messageKey));
    handler.setTopicExpression(new LiteralExpression("logs"));
    handler.setMessageKeyExpression(new LiteralExpression("logs.key"));
    return handler;
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> properties = defaultKafkaProps();
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    properties.put(ProducerConfig.RETRIES_CONFIG, 0);
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    return new DefaultKafkaProducerFactory<>(properties);
  }


  private Map<String, Object> defaultKafkaProps() {
    Map<String, Object> properties = new HashMap<>();
    List<String> hostPorts = new ArrayList<>();

    String kafkaUrl = getenv("KAFKA_URL");
    for (String url : kafkaUrl.split(",")) {
      try {
        URI uri = new URI(url);
        hostPorts.add(String.format("%s:%d", uri.getHost(), uri.getPort()));

        switch (uri.getScheme()) {
          case "kafka":
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
            break;
          case "kafka+ssl":
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            try {
              EnvKeyStore envTrustStore = EnvKeyStore.createWithRandomPassword("KAFKA_TRUSTED_CERT");
              EnvKeyStore envKeyStore = EnvKeyStore.createWithRandomPassword("KAFKA_CLIENT_CERT_KEY", "KAFKA_CLIENT_CERT");

              properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, envTrustStore.type());
              properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, envTrustStore.storeTemp().getAbsolutePath());
              properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, envTrustStore.password());
              properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, envKeyStore.type());
              properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, envKeyStore.storeTemp().getAbsolutePath());
              properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, envKeyStore.password());
            } catch (Exception e) {
              throw new RuntimeException("There was a problem creating the Kafka key stores", e);
            }
            break;
          default:
            throw new IllegalArgumentException(format("unknown scheme; %s", uri.getScheme()));
        }
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.join(",", hostPorts));
    return properties;
  }

  private String getenv(String var) {
    String val = System.getenv(var);
    if (val == null) throw new IllegalArgumentException("Env var $" + var + " is not set!");
    return val;
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HerokuReplayApplication.class, args);
  }

}