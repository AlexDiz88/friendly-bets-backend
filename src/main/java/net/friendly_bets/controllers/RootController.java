package net.friendly_bets.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @Value("${app.root-redirect-url}")
    private String redirectUrl;

    @GetMapping("/")
    public String redirect() {
        return "redirect:/" + redirectUrl;
    }
}
