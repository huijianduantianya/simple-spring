package com.jxufe.simplespring.service;

import com.jxufe.simplespring.annotation.JCService;

@JCService
public class TestService {

	public String sayHello(String name){
		return "hello " + name + ",welcome to spring";
	}
	
}
