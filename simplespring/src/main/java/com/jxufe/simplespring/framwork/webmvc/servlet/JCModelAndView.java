package com.jxufe.simplespring.framwork.webmvc.servlet;

import java.util.Map;

public class JCModelAndView {
    //页面名称
    private String viewName;

    private Map<String, ?> model;

    public JCModelAndView(String viewName) {
        this.viewName = viewName;
    }

    public JCModelAndView(String viewName, Map<String, ?> model) {
        this.viewName = viewName;
        this.model = model;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, ?> getModel() {
        return model;
    }

    public void setModel(Map<String, ?> model) {
        this.model = model;
    }
}
