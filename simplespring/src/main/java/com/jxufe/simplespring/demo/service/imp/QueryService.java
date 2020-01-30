package com.jxufe.simplespring.demo.service.imp;

import com.jxufe.simplespring.demo.service.IQueryService;
import com.jxufe.simplespring.framwork.annotation.JCService;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

@JCService
@Slf4j
public class QueryService implements IQueryService {

	public String sayHello(String name){
		return "hello " + name + ",welcome to spring";
	}

	/**
	 * 查询
	 */
	public String query(String name) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		String json = "{name:\"" + name + "\",time:\"" + time + "\"}";
		log.info("这是在业务方法中打印的：" + json);
		return json;
	}
}
