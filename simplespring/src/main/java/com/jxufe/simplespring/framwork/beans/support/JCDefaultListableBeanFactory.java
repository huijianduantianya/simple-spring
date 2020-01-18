package com.jxufe.simplespring.framwork.beans.support;

import com.jxufe.simplespring.framwork.beans.config.JCBeanDefinition;
import com.jxufe.simplespring.framwork.context.support.JCAbstractApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JCDefaultListableBeanFactory extends JCAbstractApplicationContext {

    //存储注册信息的BeanDefinition,伪IOC容器
    protected final Map<String, JCBeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

}
