package com.finalproject.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServicePolicyController {
    @GetMapping("/service-policy")
    public String policy() { return "service-policy"; }
}
