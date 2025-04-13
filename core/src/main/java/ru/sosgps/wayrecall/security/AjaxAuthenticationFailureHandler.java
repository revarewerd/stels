package ru.sosgps.wayrecall.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import ru.sosgps.wayrecall.core.Translations;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.08.12
 * Time: 23:50
 * To change this template use File | Settings | File Templates.
 */
public class AjaxAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    Translations tr;

    Logger logger = LoggerFactory.getLogger(AjaxAuthenticationFailureHandler.class);


    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        if (!"true".equalsIgnoreCase(request.getParameter("autoredirect"))) {
            response.setContentType("application/json;charset=utf-8");
            try (PrintWriter out = response.getWriter()) {
                String localizedMessage = exception.getLocalizedMessage();
                if(exception instanceof org.springframework.security.authentication.BadCredentialsException)
                    //localizedMessage = "Неверный логин и/или пароль";
                    localizedMessage = tr.getLocaleForRequest(request).tr("login.invalidloginpassword");
                out.println("{success: false, message: '" + localizedMessage + "'}");
            }
        } else {
            response.sendRedirect("/login.html");
        }

    }
}
