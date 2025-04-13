package ru.sosgps.wayrecall.m2msms.httpreceiver;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import ru.sosgps.wayrecall.sms.SMS;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Created by nickl on 24.03.14.
 */
public class JettyServer {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JettyServer.class);
    private int port = 5093;

    private Server server;


//    public static void main(String[] args) throws Exception {
//
//        final JettyServer jettyServer = new JettyServer();
//
//        jettyServer.startServer();
//        jettyServer.join();
//    }

    public void join() throws InterruptedException {
        server.join();
    }

    public Server startServer() throws Exception {
//        server = new Server(new QueuedThreadPool(8));
//        ServerConnector connector=new ServerConnector(server);
//        connector.setPort(port);
//        server.setConnectors(new Connector[]{connector});

        server = new Server(port);
        ServletContextHandler handler = new ServletContextHandler();

        server.setHandler(handler);
        handler.addServlet(new ServletHolder(new ProcessingServlet()), "/smsreceiver/*");

        server.start();
        return server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @SuppressWarnings("serial")
    class ProcessingServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            log.debug("processing request " + request.getQueryString() + " " + request.getParameterMap());
            response.setContentType("text/xml");
            response.setStatus(HttpServletResponse.SC_OK);

            try {
                String message = request.getParameter("message");
                String msid = request.getParameter("msid");
                publishJMS(new SMS(-10L, message, true, msid, "", new Date(), new Date(), SMS.DELIVERED()));
            } catch (Exception e) {
                log.error("error in request processing", e);
            }

            try (PrintWriter out = response.getWriter()) {
                out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                out.println("<OperationResult xmlns=\"http://mcommunicator.ru/M2M/\">");
                out.println("<code>0</code>");
                out.println("<description>OK</description>");
                out.println("</OperationResult>");
            }
        }
    }

    @Autowired
    private JmsTemplate jmsTemplate = null;

    private void publishJMS(final java.io.Serializable data) {
        try {
            jmsTemplate.send("smses.httpreceived", new MessageCreator() {

                @Override
                public Message createMessage(Session session) throws JMSException {
                    return session.createObjectMessage(data);
                }

            });
        } catch (Exception e) {
            log.error("error in data retranslation ", e);
        }
    }


}
