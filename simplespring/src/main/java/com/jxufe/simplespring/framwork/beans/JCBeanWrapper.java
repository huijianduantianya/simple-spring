package com.jxufe.simplespring.framwork.beans;

public class JCBeanWrapper {

    private Object wrappedInstance;

    private Class<?> wrappedClass;

    public JCBeanWrapper(Object wrappedInstance){
        this.wrappedInstance = wrappedInstance;
    }

    public Object getWrappedInstance(){
        return this.wrappedInstance;
    }

    //返回代理以后的Class
    //可能会是这个$Proxy0
    public Class<?> getWrappedClass(){
        return this.wrappedInstance.getClass();
    }

}
