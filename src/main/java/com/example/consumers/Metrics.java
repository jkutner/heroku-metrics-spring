package com.example.consumers;

import com.example.Route;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;

public class Metrics extends AbstractLogConsumer {

  private JedisPool pool;

  public static void main(String[] args) throws URISyntaxException {
    new Metrics().start();
  }

  public Metrics() throws URISyntaxException {
    if(System.getenv("REDIS_URL") == null) {
      throw new IllegalArgumentException("No REDIS_URL is set!");
    }
    URI redisUri = new URI(System.getenv("REDIS_URL"));

    pool = new JedisPool(redisUri);
  }

  public void receive(Route route) {
    String path = route.get("path");
    String pathDigest = "";

    try (Jedis jedis = pool.getResource()) {
      jedis.hset("routes", path, pathDigest);

      for (String metric : Arrays.asList("service", "connect")) {
        Integer value = Integer.valueOf(route.get(metric));
        String key = pathDigest + "::" + metric;

        jedis.hincrBy(key, "sum", value);
        jedis.hincrBy(key, "count", 1);

        Integer sum = Integer.valueOf(jedis.hget(key, "sum"));
        Float count = Float.valueOf(jedis.hget(key, "count"));
        Float avg = sum / count;
        jedis.hset(key, "average", String.valueOf(avg));
      }

      jedis.hincrBy(pathDigest + "::statuses", route.get("status"), 1);
    }
  }


}
