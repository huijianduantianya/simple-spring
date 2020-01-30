package com.jxufe.simplespring.framwork.aop.intercept;

public interface JCMethodInterceptor {

    public Object invoke(JCMethodInvocation invocation) throws Throwable;

}
