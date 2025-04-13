/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.security;

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.sosgps.wayrecall.utils.ExtDirectService;

/**
 * @author ИВАН
 */
@ExtDirectService
public class LoginService {

    @Autowired
    RememberMeServices rememberMeServices;
    
    @ExtDirectMethod
    public String getLogin() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @ExtDirectMethod
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        CookieClearingLogoutHandler cookieClearingLogoutHandler = new CookieClearingLogoutHandler(AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY);
        SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
        cookieClearingLogoutHandler.logout(request, response, null);
        securityContextLogoutHandler.logout(request, response, null);

//        HttpSession a = getSession();
//        a.invalidate();
//        SecurityContextHolder.clearContext();

    }
}
