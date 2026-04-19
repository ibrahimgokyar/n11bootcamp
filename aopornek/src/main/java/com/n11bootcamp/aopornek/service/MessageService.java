package com.n11bootcamp.aopornek.service;

import org.springframework.stereotype.Service;

@Service
public class MessageService {

    public String mesajVer(String param)
    {
        System.out.println("Metod parametresi " + param);
        return param;
    }

}
