package com.cmict.internalpaas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/debug")
public class ButtonDebugController {
    
    @GetMapping("/buttons")
    public String debugButtons() {
        return "debug/button-test";
    }
    
    @GetMapping("/server-buttons")
    public String debugServerButtons() {
        return "debug/server-buttons-test";
    }
}