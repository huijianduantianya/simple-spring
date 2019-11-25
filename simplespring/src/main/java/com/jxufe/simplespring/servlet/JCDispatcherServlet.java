package com.jxufe.simplespring.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
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

import com.jxufe.simplespring.annotation.*;

public class JCDispatcherServlet extends HttpServlet{
	
	/**
	 * ���������ļ�������
	 */
	private Properties contextConfig = new Properties();
	
	/**
	 * ����ɨ�赽������
	 */
	private List<String> classNameList = new ArrayList<>();
	
	//IOC����.Ϊ�˼򻯳�����ʱ������ConcurrentHashMap
	private Map<String, Object> ioc = new HashMap<>();
	
	private List<HandlerMapping> handlerMapping = new ArrayList<>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//6�����ã����н׶�
		try{
			doDispatch(req, resp);
		}catch(Exception e){
			e.printStackTrace();
			resp.getWriter().write("500 Exception, Detail:" + Arrays.toString(e.getStackTrace()) );
		}
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{


		HandlerMapping handlerMapping = getHandler(req);
		if(null == handlerMapping){
			resp.getWriter().write("404 not found!");
			return;
		}

		//��ȡ�������β��б�
		Class<?>[] paramTypes = handlerMapping.getParamTypes();

		Object[] paramValues = new Object[paramTypes.length];

		Map<String, String[]> params = req.getParameterMap();

		for (Entry<String, String[]> param : params.entrySet()) {
			String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
					.replaceAll("\\s", ",");

			if(!handlerMapping.paramIndexMapping.containsKey(param.getKey())){
				continue;
			}

			int index = handlerMapping.paramIndexMapping.get(param.getKey());
			paramValues[index] = convert(paramTypes[index], value);
		}

		if(handlerMapping.paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
			int reqIndex = handlerMapping.paramIndexMapping.get(HttpServletRequest.class.getName());
			paramValues[reqIndex] = req;
		}

		if(handlerMapping.paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
			int respIndex = handlerMapping.paramIndexMapping.get(HttpServletResponse.class.getName());
			paramValues[respIndex] = resp;
		}

		Object returnValue = handlerMapping.method.invoke(handlerMapping.controller, paramValues);
		if(null == returnValue || returnValue instanceof Void){
			return;
		}
		resp.getWriter().write(returnValue.toString());
	}

	private HandlerMapping getHandler(HttpServletRequest req) {
		if(handlerMapping.isEmpty()){
			return null;
		}

		//�^��·��
		String url = req.getRequestURI();
		//̎�������·��
		String contextPath = req.getContextPath();
		url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

		for (HandlerMapping mapping : this.handlerMapping) {
			if(mapping.getUrl().equals(url)){
				return mapping;
			}
		}
		return null;
	}

	//url�������Ĳ�������string���͵�
	//ֻ���Stringת��Ϊ�������ͼ���
	private Object convert(Class<?> type, String value){
		if(Integer.class == type){
			return Integer.valueOf(value);
		}
		if(Double.class == type){
			return Double.valueOf(value);
		}
		//�����������͵Ļ���������if
		//����ʹ�ò���ģʽ���ݲ�ʵ��
		return value;
	}

	@Override
	public void init(ServletConfig config) {
		
		//1�����������ļ�
		doLoanConfig(config.getInitParameter("contextConfigLocation"));
		
		//2��ɨ����ص���
		doScanner(contextConfig.getProperty("scanPackage"));
		
		//3����ʼ��ɨ�赽���࣬���ҽ����Ƿ���ICO����֮��
		doInstance();
		
		//4���������ע��
		doAutowired();
		
		//5����ʼ��HandlerMapping
		initHandlerMapping();
		
		System.out.println("111");
		
	}

	//��ʼ��url��method�Ķ�Ӧ��ϵ
	private void initHandlerMapping() {
		if(ioc.isEmpty()){
			return;
		}
		
		for(Entry<String, Object> entry: ioc.entrySet()){
			Class<?> clazz = entry.getValue().getClass();
			
			if(!clazz.isAnnotationPresent(JCController.class)){
				continue;
			}
			
			//��ȡд�������ϵ�  url
			String baseUrl = "";
			if(clazz.isAnnotationPresent(JCRequestMapping.class)){
				JCRequestMapping requestMapping = clazz.getAnnotation(JCRequestMapping.class);
				baseUrl = requestMapping.value();
			}
			
			//��ȡд�ڷ����ϵ�url
			for(Method method : clazz.getMethods()){
				if(!method.isAnnotationPresent(JCRequestMapping.class)){
					continue;
				}
				JCRequestMapping requestMapping = method.getAnnotation(JCRequestMapping.class);
				//�����򣬽����б�ܸĳ�һ��
				String url = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");

				this.handlerMapping.add(new HandlerMapping(url, method, entry.getValue()));
//				handlerMapping.put(url, method);
				System.out.println("Mapped:" + url +"," + method);
			}
			
		}
				
	}

	private void doAutowired() {
		if(ioc.isEmpty()) {
			return;
		}
		for(Map.Entry<String, Object> entry : ioc.entrySet()) {
			//��ȡ���еģ��ض����ֶΣ�����private/protected/default
			Field[] fields =  entry.getValue().getClass().getDeclaredFields();
			for(Field field : fields) {
				if(!field.isAnnotationPresent(JCAutowired.class)) {
					continue;
				}
				
				JCAutowired autowired = field.getAnnotation(JCAutowired.class);
				
				//����û�û���Զ���beanName��Ĭ�ϸ�������ע��
				//TODO ����������ĸСд���ж�
				String beanName = autowired.value().trim();
				if("".equals(beanName)) {
					beanName = toLowerFirstCase(field.getType().getSimpleName());
				}
				
				//�����public��������η���ֻҪ����@Autowiredע�⣬��Ҫ��ֵ
				//��������
				field.setAccessible(true);
				
				try{
					//���䣬��̬���ֶθ�ֵ
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
				//��Щ����Ҫ��ʼ����
				//����ע�����ų�ʼ��������жϣ�
				//Ϊ�˼򻯴����߼����Ͳ���������ע����
				//����service�����Լ����������
				if(clazz.isAnnotationPresent(JCController.class)) {
					Object instance = clazz.newInstance();
					//key classname����ĸСд
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					ioc.put(beanName, instance);
				}else if(clazz.isAnnotationPresent(JCService.class)) {
					JCService service = clazz.getAnnotation(JCService.class);
					
					
					//1.�Լ�����name�����
					String beanName = service.value();
					
					if("".equals(beanName.trim())) {
						//2.Ĭ����������ĸСд
						beanName = toLowerFirstCase(clazz.getSimpleName());
					}
					
					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					//3.���������Զ���ֵ�����ýӿڵ�ʵ����ѭ�����������ʵ������ʱ������
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

	//ɨ�����ص���
	private void doScanner(String scanPackage) {
		//scanPackage=com.jxufe.simplespring �洢���ǰ�·��		
		URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.","/"));
		File classPath = new File(url.getFile());
		for(File file : classPath.listFiles()) {
			if(file.isDirectory()) {
				//���ļ��У����еݹ�
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
		//����·�����ҵ�Spring�������ļ����ڵ��ļ�
		//���ҽ����ȡ�����ŵ�Properties��
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

	public class HandlerMapping {
		private String url;
		private Method method;
		private Object controller;

		private Class<?>[] paramTypes;

		//�β��б�
		//����������Ϊkey������˳����Ϊֵ
		private Map<String, Integer> paramIndexMapping;

		public HandlerMapping(String url, Method method, Object controller) {
			this.url = url;
			this.method = method;
			this.controller = controller;

			paramTypes = method.getParameterTypes();

			paramIndexMapping = new HashMap<>();
			putParamIndexMapping(method);
		}

		private void putParamIndexMapping(Method method){
			//�õ������ϵ�ע�⣬�õ�һ����ά����
			//��Ϊһ�����������ж��ע�⣬��һ���������ж������
			Annotation[][] pa = method.getParameterAnnotations();

			for(int i = 0; i < pa.length; i++){
				for(Annotation a : pa[i]) {
					if (a instanceof JCRequestParam) {
						//�õ��������ƣ�ȥlocalhost:8080/simplespring/jxufe/sayHello?name=shenzhenƥ��
						String paramName = ((JCRequestParam) a).value();
						//��req�õ���������ȥ�Ҷ�Ӧ��key
						if (!"".equals(paramName.trim())) {
							paramIndexMapping.put(paramName, i);
						}
					}
				}
			}


			//��ȡ�����е�request��response����
			Class<?>[] parameterTypes = method.getParameterTypes();

			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> type = parameterTypes[i];
				//������instanceof��parameterTypes����ʵ�Σ����β�
				if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
					paramIndexMapping.put(type.getName(), i);
				}
			}
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public Method getMethod() {
			return method;
		}

		public void setMethod(Method method) {
			this.method = method;
		}

		public Object getController() {
			return controller;
		}

		public void setController(Object controller) {
			this.controller = controller;
		}

		public Map<String, Integer> getParamIndexMapping() {
			return paramIndexMapping;
		}

		public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
			this.paramIndexMapping = paramIndexMapping;
		}

		public Class<?>[] getParamTypes() {
			return paramTypes;
		}

		public void setParamTypes(Class<?>[] paramTypes) {
			this.paramTypes = paramTypes;
		}
	}

}
