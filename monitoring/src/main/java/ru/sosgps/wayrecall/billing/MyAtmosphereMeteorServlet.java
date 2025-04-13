/*
 * Copyright 2013 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ru.sosgps.wayrecall.billing;

import org.atmosphere.config.service.MeteorService;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.Meteor;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Simple PubSub resource that demonstrate many functionality supported by
 * Atmosphere JQuery Plugin (WebSocket, Comet) and Atmosphere Meteor extension.
 *
 * @author Jeanfrancois Arcand
 */
@MeteorService(broadcasterCache = org.atmosphere.cache.UUIDBroadcasterCache.class)
public class MyAtmosphereMeteorServlet extends HttpServlet {

    private static Logger log = LoggerFactory.getLogger(MyAtmosphereMeteorServlet.class);

    public MyAtmosphereMeteorServlet() {

//        Timer timer = new Timer();

//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//
//                BroadcasterFactory.getDefault().lookup("123", true).broadcast("ttrrr5");
//            }
//        },1000,1000);


    }

    protected BroadcasterFactory getBroadcasterFactory() {
        final BroadcasterFactory broadcasterFactory = (BroadcasterFactory) this.getServletContext().getAttribute(BroadcasterFactory.class.getName());

        log.debug("getting broadcasterFactory servlet: " + System.identityHashCode(this) + " servletContext: " +
                System.identityHashCode(this.getServletContext()) + " broadcasterFactory=" + System.identityHashCode(broadcasterFactory));
        return Objects.requireNonNull(
                broadcasterFactory, "BroadcasterFactory"
        );
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Create a Meteor
        Meteor m = Meteor.build(req);
        // Log all events on the console, including WebSocket events.
        m.addListener(new WebSocketEventListenerAdapter());

        res.setContentType("text/html;charset=ISO-8859-1");
        log.debug("doGet called uri " + req.getServerName() + req.getRequestURI());
        Broadcaster b = lookupBroadcaster(req.getPathInfo());
        m.setBroadcaster(b);

        Meteor suspend = m.suspend(-1);
        log.debug("get called for " + req.getRemoteAddr() + " user agent=" + req.getHeader("User-Agent") + " suspend="
                + (suspend != null ? suspend.getAtmosphereResource() : null));

    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Broadcaster b = lookupBroadcaster(req.getPathInfo());

        String message = req.getReader().readLine();

        while (message != null && message.contains("message")) {
            String str = message.substring("message=".length());
            log.debug("active atmosphere resources: " + b.getAtmosphereResources());
            b.broadcast(str);
            log.debug("post called for " + req.getRemoteAddr() + " user agent=" + req.getHeader("User-Agent") + " " +
                    "data=" + str);
            message = req.getReader().readLine();
        }
    }

    Broadcaster lookupBroadcaster(String pathInfo) {
        String[] decodedPath = pathInfo.split("/");
        Broadcaster b;
        if (decodedPath.length > 0) {
            b = getBroadcasterFactory().lookup(decodedPath[decodedPath.length - 1], true);
        } else {
            b = getBroadcasterFactory().lookup("/", true);
        }
        log.debug("getting broadcaster:" + System.identityHashCode(b) + " :" + b);
        return b;
    }
}
