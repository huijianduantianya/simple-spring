package com.jxufe.simplespring.framwork.aop.support;

import com.jxufe.simplespring.framwork.aop.aspect.JCAfterReturningAdviceInterceptor;
import com.jxufe.simplespring.framwork.aop.aspect.JCAfterThrowingAdviceInterceptor;
import com.jxufe.simplespring.framwork.aop.aspect.JCMethodBeforeAdviceInterceptor;
import com.jxufe.simplespring.framwork.aop.config.JCAopConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 回调通知扩展类
 */
public class JCAdvisedSupport {

    private Class<?> targetClass;

    private Object target;

    private JCAopConfig config;

    private Pattern pointCutClassPattern;

    //方法对应的执行器链
    private transient Map<Method, List<Object>> methodCache;

    public JCAdvisedSupport(JCAopConfig config) {
        this.config = config;
    }

    public Class<?> getTargetClass(){
        return this.targetClass;
    }

    public Object getTarget(){
        return this.target;
    }

    /**
     * 获取方法执行链，比如：before->targetMethod->after
     * @param method
     * @param targetClass
     * @return
     * @throws Exception
     */
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) throws Exception{
        List<Object> cached = methodCache.get(method);
        if(cached == null){
            Method m = targetClass.getMethod(method.getName(),method.getParameterTypes());
            cached = methodCache.get(m);
            //底层逻辑，对代理方法进行一个兼容处理
            this.methodCache.put(m,cached);
        }

        return cached;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
        parse();
    }

    /**
     * 解析配置文件
     */
    private void parse() {
        //pointCut:   public .* com.jxufe.simplespring.demo.service..*Service..*(.*)
        String pointCut = config.getPointCut()
                .replaceAll("\\.","\\\\.")
                .replaceAll("\\\\.\\*",".*")
                .replaceAll("\\(","\\\\(")
                .replaceAll("\\)","\\\\)");
        String pointCutForClassRegx = pointCut.substring(0, pointCut.lastIndexOf("\\(") - 4);

        //com.jxufe.simplespring.demo.service
        pointCutClassPattern = Pattern.compile("class " + pointCutForClassRegx.substring(
                pointCutForClassRegx.lastIndexOf(" ") + 1
        ));

        try{
            methodCache = new HashMap<>();
            Pattern pattern = Pattern.compile(pointCut);

            Class aspectClass =  Class.forName(this.config.getAspectClass());
            Map<String, Method> aspectMethods = new HashMap<>();
            for (Method m : aspectClass.getMethods()) {
                aspectMethods.put(m.getName(), m);
            }


            for (Method m : this.targetClass.getMethods()) {
                String methodString = m.toString();
                if(methodString.contains("throws")){
                    methodString = methodString.substring(0, methodString.lastIndexOf("throws")).trim();
                }

                Matcher matcher = pattern.matcher(methodString);
                if (matcher.matches()){
                    //执行器链
                    List<Object> advices = new LinkedList<>();

                    //把每一个方法包装成MethodIntercept
                    //before
                    if(!(null ==config.getAspectBefore() || "".equals(config.getAspectBefore()))){
                        advices.add(new JCMethodBeforeAdviceInterceptor(aspectMethods.get(config.getAspectBefore()),aspectClass.newInstance()));
                    }
                    //after
                    if(!(null ==config.getAspectAfter() || "".equals(config.getAspectAfter()))){
                        advices.add(new JCAfterReturningAdviceInterceptor(aspectMethods.get(config.getAspectAfter()),aspectClass.newInstance()));
                    }
                    //afterThrowing
                    if(!(null ==config.getAspectAfterThrow() || "".equals(config.getAspectAfterThrow()))){
                        JCAfterThrowingAdviceInterceptor throwingAdvice =
                                new JCAfterThrowingAdviceInterceptor(aspectMethods.get(config.getAspectAfterThrow()),aspectClass.newInstance());
                        throwingAdvice.setThrowName(config.getAspectAfterThrowingName());
                        advices.add(throwingAdvice);
                    }
                    methodCache.put(m, advices);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public boolean pointCutMatch() {
        return pointCutClassPattern.matcher(this.targetClass.toString()).matches();
    }
}
