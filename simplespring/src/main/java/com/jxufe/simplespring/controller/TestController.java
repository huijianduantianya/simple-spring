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
			e.printStackTrace();
		}
	}

	@JCRequestMapping("/add")
	public void add(HttpServletRequest req, HttpServletResponse resp,@JCRequestParam("a") Integer a,@JCRequestParam("b") Integer b){
		try {
			resp.getWriter().write(a + " + "+b+" = " + (a + b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@JCRequestMapping("/sub")
	public void sub(HttpServletRequest req, HttpServletResponse resp,@JCRequestParam("a") Double a,@JCRequestParam("b") Double b){
		try {
			resp.getWriter().write(a + " - "+b+" = " + (a - b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@JCRequestMapping("/remove")
	public String remove(@JCRequestParam("id") String id){
		return id;
	}
}
