package com.jxufe.simplespring.framwork.beans.support;

import com.jxufe.simplespring.framwork.beans.config.JCBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 读取配置文件
 */
public class JCBeanDefinitionReader {

    private Properties config = new Properties();

    private final String SCAN_PACKAGE = "scanPackage";

    private List<String> registyBeanClasses = new ArrayList<>();

    public JCBeanDefinitionReader(String... locations){
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(locations[0].replace("classpath:",""));
        try {
            config.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        doScanner(config.getProperty(SCAN_PACKAGE));
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getResource("/" + scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }else{
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String className = scanPackage + "." + file.getName().replace(".class","");
                registyBeanClasses.add(className);
            }
        }
    }

    public Properties getConfig(){
        return config;
    }

    /**
     * 把配置文件中扫描到的所有的配置信息转换为JCBeanDefinition，便于之后IOC操作
     * @return
     */
    public List<JCBeanDefinition> loadBeanDefinitions(){
        List<JCBeanDefinition> result = new ArrayList<>();
        try{
            for(String className : registyBeanClasses){
                Class<?> beanClass = Class.forName(className);
                //如果是一个接口，是不能实例化的
                //用它的实现类来实例化
                if(beanClass.isInterface()){
                    continue;
                }

                //beanName有三种情况
                //1.默认是类名首字母小写
                //2.自定义名字
                //3.接口注入
                result.add(doCreateBeanDefinition(toLowerFirstCase(beanClass.getSimpleName()),beanClass.getName()));
                //可以根据类名来获取
                //result.add(doCreateBeanDefinition(beanClass.getName(),beanClass.getName()));

                Class<?> [] interfaces = beanClass.getInterfaces();
                for(Class<?> i : interfaces){
                    result.add(doCreateBeanDefinition(i.getName(), beanClass.getName()));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 把每一个配置信息解析成一个BeanDefinition
     * @param factoryBeanName
     * @param beanClassName
     * @return
     */
    private JCBeanDefinition doCreateBeanDefinition(String factoryBeanName,String beanClassName){
        JCBeanDefinition beanDefinition = new JCBeanDefinition();
        beanDefinition.setBeanClassName(beanClassName);
        beanDefinition.setFactoryBeanName(factoryBeanName);
        return beanDefinition;
    }

    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }

}
