package com.vso.DaddyJohn.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
public class HelloWorld {

    @GetMapping("/hello")
    public String hello(){
        return "hello world";
    }
}
