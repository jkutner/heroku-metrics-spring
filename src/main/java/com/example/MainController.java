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
import java.util.Map;

@Controller
@Description("A controller for handling requests for hello messages")
public class MainController implements BeanFactoryAware {

  private BeanFactory context;

  @Autowired
  private HelloWorldService helloWorldService;

  @Override
  public void setBeanFactory(BeanFactory factory) {
    this.context = factory;
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, String> hello() {
    return Collections.singletonMap("message",
        this.helloWorldService.getHelloMessage());
  }


  @RequestMapping(value = "/logs", method = RequestMethod.POST)
  @ResponseBody
  public String logs(@RequestBody String body) throws IOException {
    RFC6587SyslogDeserializer parser = new RFC6587SyslogDeserializer();

    Map<String, ?> messages = parser.deserialize(new ByteArrayInputStream(body.getBytes()));

    ObjectMapper mapper = new ObjectMapper();

    MessageChannel toKafka = context.getBean("toKafka", MessageChannel.class);

    String json = mapper.writeValueAsString(messages);
    System.out.println("Sending message: " + json);
    toKafka.send(new GenericMessage<>(json));

    return "ok";
  }

}