package com.example;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.integration.syslog.RFC5424SyslogParser;

@Controller
@Description("A controller for handling requests for hello messages")
public class MainController {

  @Autowired
  private HelloWorldService helloWorldService;

  @RequestMapping(value = "/", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, String> hello() {
    return Collections.singletonMap("message",
        this.helloWorldService.getHelloMessage());
  }

  @RequestMapping(value = "/logs", method = RequestMethod.POST)
  @ResponseBody
  public String logs(@RequestBody String body) {
    RFC5424SyslogParser parser = new RFC5424SyslogParser();

    Map<String, ?> messages = parser.parse(body, 0, false);

    for (String key : messages.keySet()) {
      System.out.println(key + ": " + messages.get(key));
    }

    return "ok";
  }

}