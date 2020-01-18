package com.jxufe.simplespring.framwork.webmvc.servlet;

import java.io.File;
import java.util.Locale;

public class JCViewResolver {

    private final String DEAULT_TEMPLATE_SUFFX = ".html";

    //文件路径
    private File templateRootDir;

    public JCViewResolver(String templateRoot) {
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        templateRootDir = new File(templateRootPath);
    }

    public JCView resolveViewName(String viewName, Locale locale){
        if(null == viewName || "".equals(viewName.trim())){
            return null;
        }

        //没写后缀，则给它补上
        viewName = viewName.endsWith(DEAULT_TEMPLATE_SUFFX) ? viewName : (viewName + DEAULT_TEMPLATE_SUFFX);

        File templateFile = new File((templateRootDir.getPath() + "/" + viewName).replaceAll("/+", "/"));
        return new JCView(templateFile);
    }
}
