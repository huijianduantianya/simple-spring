package com.jxufe.simplespring.framwork.context;

import com.jxufe.simplespring.framwork.annotation.JCAutowired;
import com.jxufe.simplespring.framwork.annotation.JCController;
import com.jxufe.simplespring.framwork.annotation.JCService;
import com.jxufe.simplespring.framwork.aop.JCAopProxy;
import com.jxufe.simplespring.framwork.aop.JCCglibAopProxy;
import com.jxufe.simplespring.framwork.aop.JCJdkDynamicAopProxy;
import com.jxufe.simplespring.framwork.aop.config.JCAopConfig;
import com.jxufe.simplespring.framwork.aop.support.JCAdvisedSupport;
import com.jxufe.simplespring.framwork.beans.JCBeanFactory;
import com.jxufe.simplespring.framwork.beans.JCBeanWrapper;
import com.jxufe.simplespring.framwork.beans.config.JCBeanDefinition;
import com.jxufe.simplespring.framwork.beans.config.JCBeanPostProcessor;
import com.jxufe.simplespring.framwork.beans.support.JCBeanDefinitionReader;
import com.jxufe.simplespring.framwork.beans.support.JCDefaultListableBeanFactory;
import com.jxufe.simplespring.framwork.context.support.JCAbstractApplicationContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class JCApplicationContext extends JCDefaultListableBeanFactory implements JCBeanFactory {

    private String[] configLocations;

    private JCBeanDefinitionReader reader;

    //单例的IOC容器缓存
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    //通用的IOC容器
    private Map<String, JCBeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

    public JCApplicationContext(String... configLocations){
        this.configLocations = configLocations;
        try {
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refresh() throws Exception{
        //1、定位配置文件
        reader = new JCBeanDefinitionReader(configLocations);

        //2、加载配置文件，扫描相关的类。把它们封装成BeanDefinition
        List<JCBeanDefinition> beanDefinitions = reader.loadBeanDefinitions();

        //3、注册，把配置信息放到容器里面（伪IOC容器）
        doRegisterBeanDefinition(beanDefinitions);

        //4、把不是延时加载的类要提前初始化
        doAutowrited();

    }

    private void doAutowrited() {
        for(Map.Entry<String, JCBeanDefinition> beanDefinitionEntry : super.beanDefinitionMap.entrySet()){
            String beanName = beanDefinitionEntry.getKey();
            if(!beanDefinitionEntry.getValue().isLazyInit()){
                try {
                    getBean(beanName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doRegisterBeanDefinition(List<JCBeanDefinition> beanDefinitions)  throws Exception{
        for(JCBeanDefinition beanDefinition : beanDefinitions){
            if(super.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())){
                throw new Exception("The “" + beanDefinition.getFactoryBeanName() + "” is exists!!");
            }
            super.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
        }
    }

    /**
     *Spring做法是，不会把最原始的对象放出去，会用一个BeanWrapper来进行一次包装
     *装饰器模式：
     *  1、保留原来的OOP关系
     *  2、需要对它进行扩展，增强（为了以后AOP打基础）
     * @param beanName
     * @return
     * @throws Exception
     */
    @Override
    public Object getBean(String beanName) throws Exception {

        //1.初始化
        Object instance = instantiateBean(beanName, this.beanDefinitionMap.get(beanName));

        //TODO （该代码应该在初始化之前，后续查看源码再处理） 这里要使用工厂模式+策略模式 扫描instance这个bean，只要实现了*Aware这个接口，就要去触发他自己的这个通知
        JCBeanPostProcessor beanPostProcessor = new JCBeanPostProcessor();
        //前置处理
        beanPostProcessor.postProcessBeforeInitialization(instance, beanName);

        //把这个对象封装到beanWrapper中
        JCBeanWrapper beanWrapper = new JCBeanWrapper(instance);

        //创建一个代理的策略，看是否用CGlib还是用JDK的代理


        //2.拿到beanWrapper后，保存到beanWrapper IOC容器中去
        this.factoryBeanInstanceCache.put(beanName, beanWrapper);

        //后置处理
        beanPostProcessor.postProcessAfterInitialization(instance, beanName);

        //3.注入
        populateBean(beanName, new JCBeanDefinition(), beanWrapper);
        return this.factoryBeanInstanceCache.get(beanName).getWrappedInstance();
    }

    private void populateBean(String beanName, JCBeanDefinition jcBeanDefinition, JCBeanWrapper jcBeanWrapper) {
        Object instance = jcBeanWrapper.getWrappedInstance();
        Class<?> clazz = jcBeanWrapper.getWrappedClass();
        //判断只有加了注解的类，才执行依赖注入
        if(!(clazz.isAnnotationPresent(JCController.class) || clazz.isAnnotationPresent(JCService.class))){
            return;
        }

        //获取所有的fields
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAnnotationPresent(JCAutowired.class)){
                continue;
            }
            JCAutowired autowired = field.getAnnotation(JCAutowired.class);
            String autowiredBeanName = autowired.value().trim();
            if("".equals(autowiredBeanName)){
                autowiredBeanName = field.getType().getName();
            }
            //强制访问
            field.setAccessible(true);

            try {
                //TODO 为啥会伪null，先留着，后续解决
                if(null == this.factoryBeanInstanceCache.get(autowiredBeanName)){
                    continue;
                }
                field.set(instance, this.factoryBeanInstanceCache.get(autowiredBeanName).getWrappedInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }

    private Object instantiateBean(String beanName, JCBeanDefinition jcBeanDefinition) {
        //1、拿到要实例化的对象的类名
        String className = jcBeanDefinition.getBeanClassName();
        //2、反射实例化，得到一个对象
        Object instance = null;
        try{
            //假设默认就是单例,细节暂时不考虑
            if(this.singletonObjects.containsKey(className)){
                instance = this.singletonObjects.get(className);
            }else{
                Class<?> clazz = Class.forName(className);
                instance = clazz.newInstance();

                JCAdvisedSupport config = instantionAopConfig(jcBeanDefinition);
                config.setTargetClass(clazz);
                config.setTarget(instance);
                //符合PointCut的规则的话，就创建代理对象
                if(config.pointCutMatch()){
                    instance = createProxy(config).getProxy();
                }

                this.singletonObjects.put(className, instance);
                this.singletonObjects.put(jcBeanDefinition.getFactoryBeanName(), instance);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return instance;
    }

    private JCAopProxy createProxy(JCAdvisedSupport config) {
        Class targetClass = config.getTargetClass();
        //这个类有实现其他接口，则使用JDK的动态代理
        if(targetClass.getInterfaces().length > 0){
            return new JCJdkDynamicAopProxy(config);
        }
        return new JCCglibAopProxy(config);
    }

    private JCAdvisedSupport instantionAopConfig(JCBeanDefinition jcBeanDefinition) {
        JCAopConfig config = new JCAopConfig();
        config.setPointCut(this.reader.getConfig().getProperty("pointCut"));
        config.setAspectClass(this.reader.getConfig().getProperty("aspectClass"));
        config.setAspectBefore(this.reader.getConfig().getProperty("aspectBefore"));
        config.setAspectAfter(this.reader.getConfig().getProperty("aspectAfter"));
        config.setAspectAfterThrow(this.reader.getConfig().getProperty("aspectAfterThrow"));
        config.setAspectAfterThrowingName(this.reader.getConfig().getProperty("aspectAfterThrowingName"));
        return new JCAdvisedSupport(config);
    }

    public String[] getBeanDefinitionNames(){
        return this.beanDefinitionMap.keySet().toArray(new String[this.beanDefinitionMap.size()]);
    }

    public int getBeanDefinitionCount(){
        return this.beanDefinitionMap.size();
    }

    public Properties getConfig(){
        return this.reader.getConfig();
    }

    /*private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }*/
}
