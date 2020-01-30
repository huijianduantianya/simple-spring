package com.jxufe.simplespring.framwork.aop.aspect;

import java.lang.reflect.Method;

public interface JCJoinPoint {
    Object getThis();

    Object[] getArguments();

    Method getMethod();

    void setUserAttribute(String key, Object value);

    Object getUserAttribute(String key);
}
