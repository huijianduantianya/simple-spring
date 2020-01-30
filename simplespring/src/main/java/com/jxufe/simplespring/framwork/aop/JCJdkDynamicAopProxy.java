package com.jxufe.simplespring.framwork.aop;

import com.jxufe.simplespring.framwork.aop.intercept.JCMethodInvocation;
import com.jxufe.simplespring.framwork.aop.support.JCAdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class JCJdkDynamicAopProxy implements JCAopProxy, InvocationHandler {

    private JCAdvisedSupport advised;

    public JCJdkDynamicAopProxy(JCAdvisedSupport advised){
        this.advised = advised;
    }

    @Override
    public Object getProxy() {
        return getProxy(this.advised.getTargetClass().getClassLoader());
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        return Proxy.newProxyInstance(classLoader,this.advised.getTargetClass().getInterfaces(),this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<Object> interceptorsAndDynamicMethodMatchers = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method,this.advised.getTargetClass());
        JCMethodInvocation invocation = new JCMethodInvocation(proxy,this.advised.getTarget(),method,args,this.advised.getTargetClass(),interceptorsAndDynamicMethodMatchers);
        return invocation.proceed();
    }
}
