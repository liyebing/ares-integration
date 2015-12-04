package com.ares.demo.spring.service;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author liyebing created on  15/9/22.
 * @version $Id$
 */
@Service
public class UserService implements IUserService{

    @Resource
    private AnotherService anotherService;

    public String queryName(String name){
        System.out.println(name);
        return name;
    }

    public int add (int a,int b){
        System.out.println(anotherService.sayHi("hello world"));
        return a+b;
    }

}
