package com.jxufe.simplespring.demo.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jxufe.simplespring.framwork.annotation.JCAutowired;
import com.jxufe.simplespring.framwork.annotation.JCController;
import com.jxufe.simplespring.framwork.annotation.JCRequestMapping;
import com.jxufe.simplespring.framwork.annotation.JCRequestParam;
import com.jxufe.simplespring.framwork.webmvc.servlet.JCModelAndView;
import com.jxufe.simplespring.demo.service.TestService;

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

	@JCRequestMapping("/query")
	public JCModelAndView query(HttpServletRequest request, HttpServletResponse response,
								@JCRequestParam("name") String name){
		String result = testService.query(name);
		return out(response,result);
	}

	private JCModelAndView out(HttpServletResponse resp, String str){
		try {
			resp.getWriter().write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@JCRequestMapping("/exception")
	public JCModelAndView exception(HttpServletRequest req, HttpServletResponse resp){
		Map<String,Object> model = new HashMap<String,Object>();
		Exception e = new Exception("错误！");
		model.put("detail",e.getMessage());
//			System.out.println(Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]",""));
		model.put("stackTrace", Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]",""));
		return new JCModelAndView("500",model);
	}

}
