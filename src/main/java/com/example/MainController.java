package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.integration.syslog.inbound.RFC6587SyslogDeserializer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
@Description("A controller for handling requests for hello messages")
public class MainController implements BeanFactoryAware {

  private BeanFactory context;

  @Override
  public void setBeanFactory(BeanFactory factory) {
    this.context = factory;
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  @ResponseBody
  public String hello() {
    return "Hello from Spring!";
  }


  @RequestMapping(value = "/logs", method = RequestMethod.POST)
  @ResponseBody
  public String logs(@RequestBody String body) throws IOException {
    System.out.println("LOG: " + body);

    // "application/logplex-1" does not conform to RFC5424. It leaves out STRUCTURED-DATA but does not replace it with
    // a NILVALUE. To workaround this, we inject empty STRUCTURED-DATA.
    String[] parts = body.split("router - ");
    String log = parts[0] + "router - [] " + (parts.length > 1 ? parts[1] : "");

    RFC6587SyslogDeserializer parser = new RFC6587SyslogDeserializer();
    Map<String, ?> messages = parser.deserialize(new ByteArrayInputStream(log.getBytes()));
    ObjectMapper mapper = new ObjectMapper();

    MessageChannel toKafka = context.getBean("toKafka", MessageChannel.class);
    String json = mapper.writeValueAsString(messages);
    toKafka.send(new GenericMessage<>(json));

    System.out.println(json);

    return "ok";
  }

}