package com.redhat.cnd;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;


@ApplicationScoped
public class GreetingService {

  @ConfigProperty(name = "application.greeting.message")
  String message;

  public String message() {
    return message;
  }
}
