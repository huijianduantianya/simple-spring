package com.jxufe.simplespring.framwork.webmvc.servlet;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class JCHandlerMapping {

    private Pattern pattern; //URL的正则匹配
    private Object controller;  //保存方法对应的实例
    private Method method;  //保存映射关系

    public JCHandlerMapping(Pattern pattern, Object controller, Method method) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
