package com.jxufe.simplespring.framwork.beans.config;

/**
 * Bean初始化事件响应，源码中是接口，这里先直接实现
 */
public class JCBeanPostProcessor {
    /**
     * 为在Bean的初始化前提供回调入口
     * @param bean
     * @param beanName
     * @return
     * @throws Exception
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception{
        return bean;
    }

    /**
     * 为在Bean的初始化后提供回调入口
     * @param bean
     * @param beanName
     * @return
     * @throws Exception
     */
    public Object postProcessAfterInitialization(Object bean, String beanName)throws Exception{
        return bean;
    }

}
