package com.jxufe.simplespring.framwork.aop;

public interface JCAopProxy {

    Object getProxy();

    Object getProxy(ClassLoader classLoader);

}
