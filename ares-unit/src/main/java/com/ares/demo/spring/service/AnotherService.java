package com.ares.demo.spring.service;

import org.springframework.stereotype.Service;

/**
 * @author liyebing created on  15/9/22.
 * @version $Id$
 */
@Service
public class AnotherService implements IAnotherService {

    public String sayHi(String hi){
        System.out.println(hi);
        return hi;
    }

}
