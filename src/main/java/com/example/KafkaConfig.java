package com.example;

import com.github.jkutner.EnvKeyStore;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static java.lang.String.format;

public class KafkaConfig {

  public static String getTopic() {
    return "logs"; //getenv("KAFKA_TOPIC");
  }

  public static String getMessageKey() {
    return "logs.key"; //getenv("KAFKA_MESSAGE_KEY");
  }

  public static Map<String, Object> consumerDefaults() {
    Map<String, Object> properties = defaultKafkaProps();
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    return properties;
  }

  public static Map<String, Object> producerDefaults() {
    Map<String, Object> properties = defaultKafkaProps();
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    properties.put(ProducerConfig.RETRIES_CONFIG, 0);
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    return properties;
  }

  private static Map<String, Object> defaultKafkaProps() {
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

  private static String getenv(String var) {
    String val = System.getenv(var);
    if (val == null) throw new IllegalArgumentException("Env var $" + var + " is not set!");
    return val;
  }
}
