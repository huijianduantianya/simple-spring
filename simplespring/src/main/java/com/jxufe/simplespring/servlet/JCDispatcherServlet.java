package com.jxufe.simplespring.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jxufe.simplespring.annotation.JCAutowired;
import com.jxufe.simplespring.annotation.JCController;
import com.jxufe.simplespring.annotation.JCRequestMapping;
import com.jxufe.simplespring.annotation.JCService;

public class JCDispatcherServlet extends HttpServlet{
	
	/**
	 * 保存配置文件的内容
	 */
	private Properties contextConfig = new Properties();
	
	/**
	 * 保存扫描到的类名
	 */
	private List<String> classNameList = new ArrayList<>();
	
	//IOC容器.为了简化程序，暂时不考虑ConcurrentHashMap
	private Map<String, Object> ioc = new HashMap<>();
	
	//保存url和method的对应关系
	private Map<String, Method> handlerMapping = new HashMap<>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//6、调用，运行阶段
		try{
			doDispatch(req, resp);
		}catch(Exception e){
			e.printStackTrace();
			resp.getWriter().write("500 Exception, Detail:" + Arrays.toString(e.getStackTrace()) );
		}
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
		//絕對路徑
		String url = req.getRequestURI();
		//處理成相對路徑
		String contextPath = req.getContextPath();
		url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
		if(!this.handlerMapping.containsKey(url)){
			resp.getWriter().write("404 not found!");
			return;
		}
		
		Method method = this.handlerMapping.get(url);
		//投機取巧的方式
		//通過反射拿到method所在class，拿到class之後在拿到class的名稱
		//在調用toLowerFirstCase獲得beanName
		String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName()) ;
		
		//暫時將參數名稱寫死
		Map<String, String[]> params = req.getParameterMap();
		System.err.println(params.get("name")[0]);
		method.invoke(ioc.get(beanName), req, resp, params.get("name")[0]);
	}

	@Override
	public void init(ServletConfig config) {
		
		//1、加载配置文件
		doLoanConfig(config.getInitParameter("contextConfigLocation"));
		
		//2、扫描相关的类
		doScanner(contextConfig.getProperty("scanPackage"));
		
		//3、初始化扫描到的类，并且将它们放入ICO容器之中
		doInstance();
		
		//4、完成依赖注入
		doAutowired();
		
		//5、初始化HandlerMapping
		initHandlerMapping();
		
		System.out.println("111");
		
	}

	//初始化url和method的对应关系
	private void initHandlerMapping() {
		if(ioc.isEmpty()){
			return;
		}
		
		for(Entry<String, Object> entry: ioc.entrySet()){
			Class<?> clazz = entry.getValue().getClass();
			
			if(!clazz.isAnnotationPresent(JCController.class)){
				continue;
			}
			
			//获取写在类名上的  url
			String baseUrl = "";
			if(clazz.isAnnotationPresent(JCRequestMapping.class)){
				JCRequestMapping requestMapping = clazz.getAnnotation(JCRequestMapping.class);
				baseUrl = requestMapping.value();
			}
			
			//获取写在方法上的url
			for(Method method : clazz.getMethods()){
				if(!method.isAnnotationPresent(JCRequestMapping.class)){
					continue;
				}
				JCRequestMapping requestMapping = method.getAnnotation(JCRequestMapping.class);
				//用正则，将多个斜杠改成一个
				String url = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
				
				handlerMapping.put(url, method);
				System.out.println("Mapped:" + url +"," + method);
			}
			
		}
				
	}

	private void doAutowired() {
		if(ioc.isEmpty()) {
			return;
		}
		for(Map.Entry<String, Object> entry : ioc.entrySet()) {
			//获取所有的，特定的字段，包括private/protected/default
			Field[] fields =  entry.getValue().getClass().getDeclaredFields();
			for(Field field : fields) {
				if(!field.isAnnotationPresent(JCAutowired.class)) {
					continue;
				}
				
				JCAutowired autowired = field.getAnnotation(JCAutowired.class);
				
				//如果用户没有自定义beanName，默认根据类型注入
				//TODO 对类名首字母小写的判断
				String beanName = autowired.value().trim();
				if("".equals(beanName)) {
					beanName = toLowerFirstCase(field.getType().getSimpleName());
				}
				
				//如果是public以外的修饰符，只要加了@Autowired注解，都要赋值
				//暴力访问
				field.setAccessible(true);
				
				try{
					//反射，动态给字段赋值
					field.set(entry.getValue(), ioc.get(beanName));
				}catch(Exception e){
					System.err.println("set value error!");
				}
			}
		}
	}

	private void doInstance() {
		if(classNameList.isEmpty()) {
			return;
		}
		try {
			for(String className : classNameList) {
				Class<?> clazz = Class.forName(className);
				//哪些类需要初始化？
				//加了注解的类才初始化，如何判断？
				//为了简化代码逻辑，就不举例其他注解了
				//假设service存在自己命名的情况
				if(clazz.isAnnotationPresent(JCController.class)) {
					Object instance = clazz.newInstance();
					//key classname首字母小写
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					ioc.put(beanName, instance);
				}else if(clazz.isAnnotationPresent(JCService.class)) {
					JCService service = clazz.getAnnotation(JCService.class);
					
					
					//1.自己命名name的情况
					String beanName = service.value();
					
					if("".equals(beanName.trim())) {
						//2.默认类名首字母小写
						beanName = toLowerFirstCase(clazz.getSimpleName());
					}
					
					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					//3.根据类型自动赋值，将该接口的实现类循环遍历，多个实现类暂时不处理
					for(Class<?>  ca : clazz.getInterfaces() ) {
						if(ioc.containsKey(toLowerFirstCase(ca.getName()))) {
							throw new Exception("");
						}
						ioc.put(toLowerFirstCase(ca.getName()), instance);
					}
				}
				
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	private String toLowerFirstCase(String simpleName) {
		char [] chars = simpleName.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	//扫描出相关的类
	private void doScanner(String scanPackage) {
		//scanPackage=com.jxufe.simplespring 存储的是包路径		
		URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.","/"));
		File classPath = new File(url.getFile());
		for(File file : classPath.listFiles()) {
			if(file.isDirectory()) {
				//是文件夹，进行递归
				doScanner(scanPackage + "." + file.getName());
			}else {
				if(!file.getName().endsWith(".class")) {
					continue;
				}
				String  className = scanPackage + "." + file.getName().replace(".class", "");
				classNameList.add(className);
			}
		}
	}

	
	private void doLoanConfig(String contextConfigLocation) {
		//从类路径下找到Spring主配置文件所在的文件
		//并且将其读取出来放到Properties中
		InputStream file = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contextConfig.load(file);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(null != file) {
				try {
					file.close();
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
