package com.api.userservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")   // base path for this controller
public class UserController {

    @GetMapping("/test")
    public String testApi() {
        return "User Service is working!";
    }
}
