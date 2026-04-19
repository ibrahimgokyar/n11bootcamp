package com.n11bootcamp.aopornek.controller;
import com.n11bootcamp.aopornek.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;



    @PostMapping
    public ResponseEntity<String> ornekMethod(@RequestBody String message) {
        return ResponseEntity.ok(messageService.mesajVer(message));
    }

}
