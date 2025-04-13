package ru.sosgps.wayrecall.monitoring.deegree;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Created by nickl on 04.09.14.
 */
public class DeegreeServletSingleton extends HttpServlet {


    private static HttpServlet wrapped = new DeegreeServlet();

    private static boolean initializated = false;

    private static boolean destroed = false;

    public HttpServlet getWrapped() {
        return wrapped;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        wrapped.service(req, resp);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        wrapped.service(req, res);
    }

    @Override
    public void destroy() {
        synchronized (DeegreeServletSingleton.class) {
            if (!destroed) {
                wrapped.destroy();
                destroed = true;
            }
        }
    }

    @Override
    public String getInitParameter(String name) {
        return wrapped.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return wrapped.getInitParameterNames();
    }

    @Override
    public ServletConfig getServletConfig() {
        return wrapped.getServletConfig();
    }

    @Override
    public ServletContext getServletContext() {
        return wrapped.getServletContext();
    }

    @Override
    public String getServletInfo() {
        return wrapped.getServletInfo();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        synchronized (DeegreeServletSingleton.class) {
            if (!initializated) {
                wrapped.init(config);
                initializated = true;
            }
        }
    }

    @Override
    public void init() throws ServletException {
        wrapped.init();
    }

    @Override
    public void log(String msg) {
        wrapped.log(msg);
    }

    @Override
    public void log(String message, Throwable t) {
        wrapped.log(message, t);
    }

    @Override
    public String getServletName() {
        return wrapped.getServletName();
    }
}
