package com.jxufe.simplespring.framwork.aop;

import com.jxufe.simplespring.framwork.aop.support.JCAdvisedSupport;

public class JCCglibAopProxy implements JCAopProxy {
    public JCCglibAopProxy(JCAdvisedSupport config) {
    }

    @Override
    public Object getProxy() {
        return null;
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        return null;
    }
}
