package com.jxufe.simplespring.framwork.aop.aspect;

import java.lang.reflect.Method;

public abstract class JCAbstractAspectAdvice implements JCAdvice{

    private Method aspectMethod;

    //切面类
    private Object aspectTarget;

    public JCAbstractAspectAdvice(Method aspectMethod, Object aspectTarget) {
        this.aspectMethod = aspectMethod;
        this.aspectTarget = aspectTarget;
    }

    public Object invokeAdviceMethod(JCJoinPoint joinPoint, Object returnValue, Throwable tx) throws Throwable{
        Class<?> [] paramTypes = this.aspectMethod.getParameterTypes();
        if(null == paramTypes || paramTypes.length == 0){
            return this.aspectMethod.invoke(aspectTarget);
        }else{
            Object[] args = new Object[paramTypes.length];
            for(int i=0; i < paramTypes.length; i++){
                if(paramTypes[i] == JCJoinPoint.class){
                    args[i] = joinPoint;
                }else if(paramTypes[i] == Throwable.class){
                    args[i] = tx;
                }else if(paramTypes[i] == Object.class){
                    args[i] = returnValue;
                }
            }
            return this.aspectMethod.invoke(aspectTarget, args);
        }
    }
}
