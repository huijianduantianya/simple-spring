package com.jxufe.simplespring.framwork.webmvc.servlet;

import com.jxufe.simplespring.framwork.annotation.JCController;
import com.jxufe.simplespring.framwork.annotation.JCRequestMapping;
import com.jxufe.simplespring.framwork.context.JCApplicationContext;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JCDispatcherServlet extends HttpServlet {

    private final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";

    private JCApplicationContext applicationContext;

    private List<JCHandlerMapping> handlerMappings = new ArrayList<>();

    //Spring中是使用list，可以兼容多个，这边暂时只兼容一个
    private Map<JCHandlerMapping, JCHandlerAdapter> handlerAdapters = new ConcurrentHashMap<>();

    private List<JCViewResolver> viewResolvers = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            doDispatch(req, resp);
        }catch (Exception e){
            //processDispatchResult(req,resp,new JCModelAndView("500"));
            log.error("doDispatcher error!",e);
            resp.getWriter().write("500 Exception, Detail:" + Arrays.toString(e.getStackTrace()) );
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        //1.通过从request中拿到url，匹配一个handlerMapping
        JCHandlerMapping handler = getHandler(req);
        if (null == handler){
            processDispatchResult(req,resp,new JCModelAndView("404"));
            return;
        }

        //2.准备调用前的参数
        JCHandlerAdapter ha = getHandlerAdapter(handler);

        //3.真正的调用方法，返回JCModelAndView存储了要传到页面上的值，和页面模板的名称
        JCModelAndView mv = ha.handle(req, resp, handler);

        //这一步才是真正的输出
        processDispatchResult(req, resp, mv);

    }

    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, JCModelAndView mv) throws Exception {
        //把ModelAndView变成一个HTML、json、OuputStream...
        if(null == mv){
            return;
        }
        if(this.viewResolvers.isEmpty()){
            return;
        }

        for (JCViewResolver viewResolver : this.viewResolvers) {
            JCView view = viewResolver.resolveViewName(mv.getViewName(), null);
            view.render(mv.getModel(),req,resp);
            return;
        }

    }

    private JCHandlerAdapter getHandlerAdapter(JCHandlerMapping handler) {
        if(this.handlerAdapters.isEmpty()){
            return null;
        }
        JCHandlerAdapter ha = this.handlerAdapters.get(handler);
        if(ha.supports(handler)){
            return ha;
        }
        return null;
    }

    private JCHandlerMapping getHandler(HttpServletRequest req) throws Exception{
        if(this.handlerMappings.isEmpty()){
            return null;
        }

        //绝对路径
        String url = req.getRequestURI();
        //处理成相对路径
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        for (JCHandlerMapping handler : this.handlerMappings) {
            try{
                Matcher matcher = handler.getPattern().matcher(url);
                if(!matcher.matches()){
                    continue;
                }
                return handler;
            }catch (Exception e){
                log.error("getHandler error! ",e);
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.初始化ApplicationContext
        applicationContext = new JCApplicationContext(config.getInitParameter(CONTEXT_CONFIG_LOCATION));
        //2.初始化SpringMvc的九大组件
        initStrategies(applicationContext);
    }

    /**
     * 初始化策略
     * @param context
     */
    protected void initStrategies(JCApplicationContext context){
        //多文件上传的组件
        initMultipartResolver(context);
        //初始化本地语言环境
        initLocaleResolver(context);
        //初始化模板处理器
        initThemeResolver(context);
        //handlerMapping，必须实现
        initHandlerMappings(context);
        //初始化参数适配器，必须实现
        initHandlerAdapters(context);
        //初始化异常拦截器
        initHandlerExceptionResolvers(context);
        //初始化视图预处理器
        initRequestToViewNameTranslator(context);
        //初始化视图转换器，必须实现
        initViewResolvers(context);
        //参数缓存器
        initFlashMapManager(context);
    }

    private void initFlashMapManager(JCApplicationContext context) {
    }

    private void initViewResolvers(JCApplicationContext context) {
        //拿到模板的存放目录
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();

        File templateRootDir = new File(templateRootPath);
        for (File template : templateRootDir.listFiles()) {
            this.viewResolvers.add(new JCViewResolver(templateRoot));
        }

    }

    private void initRequestToViewNameTranslator(JCApplicationContext context) {
    }

    private void initHandlerExceptionResolvers(JCApplicationContext context) {
    }

    private void initHandlerAdapters(JCApplicationContext context) {
        //把一个request请求变成一个handler，参数都是字符串的，自动配到handler中的形参
        //要拿到HandlerMapping才能干活,意味着有几个HandlerMapping，就有几个HandlerAdapter
        for(JCHandlerMapping handlerMapping : this.handlerMappings){
            this.handlerAdapters.put(handlerMapping, new JCHandlerAdapter());
        }

    }

    private void initHandlerMappings(JCApplicationContext context) {
        String[] beanNames = context.getBeanDefinitionNames();
        try{
            for (String beanName : beanNames) {
                Object controller = context.getBean(beanName);
                Class<?> clazz = controller.getClass();
                if(!clazz.isAnnotationPresent(JCController.class)){
                    continue;
                }


                //获取写在类名上的  url
                String baseUrl = "";
                if(clazz.isAnnotationPresent(JCRequestMapping.class)){
                    JCRequestMapping requestMapping = clazz.getAnnotation(JCRequestMapping.class);
                    baseUrl = requestMapping.value();
                }

                //获取写在方法上的url
                for(Method method : clazz.getMethods()){
                    if(!method.isAnnotationPresent(JCRequestMapping.class)){
                        continue;
                    }
                    JCRequestMapping requestMapping = method.getAnnotation(JCRequestMapping.class);
                    //用正则，将多个斜杠改成一个
                    String regex = (baseUrl + "/" + requestMapping.value().replaceAll("\\*",".*")).replaceAll("/+", "/");
                    Pattern pattern = Pattern.compile(regex);

                    this.handlerMappings.add(new JCHandlerMapping(pattern, controller, method));
                    log.info("Mapped:" + regex +"," + method);
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void initThemeResolver(JCApplicationContext context) {
    }

    private void initLocaleResolver(JCApplicationContext context) {
    }

    private void initMultipartResolver(JCApplicationContext context) {
    }
}
