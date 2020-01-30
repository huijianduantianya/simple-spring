package com.jxufe.simplespring.framwork.aop.aspect;

import com.jxufe.simplespring.framwork.aop.intercept.JCMethodInterceptor;
import com.jxufe.simplespring.framwork.aop.intercept.JCMethodInvocation;

import java.lang.reflect.Method;

public class JCMethodBeforeAdviceInterceptor extends JCAbstractAspectAdvice implements JCAdvice,JCMethodInterceptor {

    private JCJoinPoint joinPoint;

    public JCMethodBeforeAdviceInterceptor(Method aspectMethod, Object aspectTarget) {
        super(aspectMethod, aspectTarget);
    }

    public void before(Method method, Object[] args, Object target) throws  Throwable{
        //传送给织入参数
        //method.invoke(target);
        super.invokeAdviceMethod(this.joinPoint, null, null);
    }

    @Override
    public Object invoke(JCMethodInvocation mi) throws Throwable {
        //从被织入得代码中才能拿到，JoinPoint
        this.joinPoint = mi;
        before(mi.getMethod(), mi.getArguments(), mi.getThis());
        return mi.proceed();
    }
}
