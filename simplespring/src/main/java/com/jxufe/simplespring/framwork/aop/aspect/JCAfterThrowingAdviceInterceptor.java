package com.jxufe.simplespring.framwork.aop.aspect;

import com.jxufe.simplespring.framwork.aop.intercept.JCMethodInterceptor;
import com.jxufe.simplespring.framwork.aop.intercept.JCMethodInvocation;

import java.lang.reflect.Method;

public class JCAfterThrowingAdviceInterceptor extends JCAbstractAspectAdvice implements JCAdvice,JCMethodInterceptor {

    private String throwingName;

    public JCAfterThrowingAdviceInterceptor(Method aspectMethod, Object aspectTarget) {
        super(aspectMethod, aspectTarget);
    }

    @Override
    public Object invoke(JCMethodInvocation mi) throws Throwable {
        try{
            return mi.proceed();
        }catch (Throwable e){
            invokeAdviceMethod(mi, null, e.getCause());
            throw e;
        }
    }

    public void setThrowName(String throwName){
        this.throwingName = throwName;
    }
}
