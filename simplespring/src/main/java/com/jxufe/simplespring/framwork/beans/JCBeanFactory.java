package com.jxufe.simplespring.framwork.beans;

/**
 * 单例工厂的顶层设计
 */
public interface JCBeanFactory {
    /**
     * 根据beanName从ioc容器中获得一个实例bean
     * @param beanName
     * @return
     */
    Object getBean(String beanName) throws Exception;
}
