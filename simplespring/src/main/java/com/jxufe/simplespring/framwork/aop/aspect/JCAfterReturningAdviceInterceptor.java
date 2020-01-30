package com.jxufe.simplespring.framwork.aop.aspect;

import com.jxufe.simplespring.framwork.aop.intercept.JCMethodInterceptor;
import com.jxufe.simplespring.framwork.aop.intercept.JCMethodInvocation;

import java.lang.reflect.Method;

public class JCAfterReturningAdviceInterceptor extends JCAbstractAspectAdvice implements JCAdvice,JCMethodInterceptor {

    private JCJoinPoint joinPoint;

    public JCAfterReturningAdviceInterceptor(Method aspectMethod, Object aspectTarget) {
        super(aspectMethod, aspectTarget);
    }

    @Override
    public Object invoke(JCMethodInvocation mi) throws Throwable {
        Object retVal = mi.proceed();
        this.joinPoint = mi;
        this.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
        return retVal;
    }

    private void afterReturning(Object retVal, Method method, Object[] arguments, Object aThis) throws Throwable {
        super.invokeAdviceMethod(this.joinPoint, retVal, null);
    }
}
