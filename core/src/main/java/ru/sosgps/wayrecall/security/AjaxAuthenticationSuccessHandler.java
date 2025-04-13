package ru.sosgps.wayrecall.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.08.12
 * Time: 23:41
 * To change this template use File | Settings | File Templates.
 */
public class AjaxAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static Logger log = LoggerFactory.getLogger(AjaxAuthenticationSuccessHandler.class);

    @Autowired
    RememberMeServices rememberMeServices;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        rememberMeServices.loginSuccess(request, response, authentication);

        if (!"true".equalsIgnoreCase(request.getParameter("autoredirect"))) {
            response.setContentType("application/json");
            try (PrintWriter out = response.getWriter()) {
                out.println("{success: true}");
            }
        } else {
            new SimpleUrlAuthenticationSuccessHandler().onAuthenticationSuccess(request, response, authentication);
        }
    }
}
