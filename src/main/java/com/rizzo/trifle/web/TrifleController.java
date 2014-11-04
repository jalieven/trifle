package com.rizzo.trifle.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TrifleController {

    @RequestMapping(value = "/exception")
    public String exception() {
        return "exception";
    }

}
