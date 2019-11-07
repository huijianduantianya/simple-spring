package com.jxufe.simplespring.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jxufe.simplespring.annotation.JCAutowired;
import com.jxufe.simplespring.annotation.JCController;
import com.jxufe.simplespring.annotation.JCRequestMapping;
import com.jxufe.simplespring.annotation.JCRequestParam;
import com.jxufe.simplespring.service.TestService;

@JCRequestMapping("/jxufe")
@JCController
public class TestController {

	@JCAutowired
	private TestService testService;
	
	@JCRequestMapping("/sayHello")
	public void sayHello(HttpServletRequest req, HttpServletResponse resp,@JCRequestParam("name") String name){
		try {
			resp.getWriter().write(testService.sayHello(name));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
