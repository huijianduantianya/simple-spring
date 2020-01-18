package com.jxufe.simplespring.framwork.webmvc.servlet;

import com.jxufe.simplespring.framwork.annotation.JCRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JCHandlerAdapter {
    public boolean supports(Object handler){
        return (handler instanceof JCHandlerMapping);
    }

    public JCModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        JCHandlerMapping handlerMapping = (JCHandlerMapping)handler;

        //形参列表
        //参数名字作为key，参数顺序作为值
        Map<String, Integer> paramIndexMapping = new HashMap<>();
        //拿到方法上的注解，得到一个二维数组
        //因为一个参数可以有多个注解，而一个方法又有多个参数
        Annotation[][] pa = handlerMapping.getMethod().getParameterAnnotations();

        for(int i = 0; i < pa.length; i++){
            for(Annotation a : pa[i]) {
                if (a instanceof JCRequestParam) {
                    //拿到参数名称，去localhost:8080/simplespring/jxufe/sayHello?name=shenzhen匹配
                    String paramName = ((JCRequestParam) a).value();
                    //从req拿到参数表中去找对应的key
                    if (!"".equals(paramName.trim())) {
                        paramIndexMapping.put(paramName, i);
                    }
                }
            }
        }
        //提取方法中的request和response参数
        Class<?>[] parameterTypes = handlerMapping.getMethod().getParameterTypes();

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            //不能用instanceof，parameterTypes不是实参，是形参
            if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                paramIndexMapping.put(type.getName(), i);
            }
        }

        Object[] paramValues = new Object[parameterTypes.length];

        //实参列表
        Map<String, String[]> params = request.getParameterMap();

        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s", ",");

            if(!paramIndexMapping.containsKey(param.getKey())){
                continue;
            }

            int index = paramIndexMapping.get(param.getKey());
            paramValues[index] = caseStringValue(value, parameterTypes[index]);
        }

        if(paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            int reqIndex = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = request;
        }

        if(paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            int respIndex = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = response;
        }

        Object result = handlerMapping.getMethod().invoke(handlerMapping.getController(), paramValues);
        if(null == result || result instanceof Void){
            return null;
        }


        boolean isModelAndView = handlerMapping.getMethod().getReturnType() == JCModelAndView.class;
        if(isModelAndView){
            return (JCModelAndView) result;
        }

        return null;
    }

    private Object caseStringValue(String value, Class<?> parameterType) {
        if(Integer.class == parameterType){
            return Integer.valueOf(value);
        }
        if(Double.class == parameterType){
            return Double.valueOf(value);
        }
        //还有其他类型的话，继续加if
        //可以使用策略模式，暂不实现
        return value;
    }
}
