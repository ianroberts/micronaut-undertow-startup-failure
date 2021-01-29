package com.example;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class ExampleController {

  @Get
  public String index() {
    return "Hello world";
  }
}
