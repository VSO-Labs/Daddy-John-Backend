package com.vso.DaddyJohn.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Service
@RestController
public class helthCheck {

    @GetMapping("/hello")
    public String hello(){
        return "hello world";
    }
}
