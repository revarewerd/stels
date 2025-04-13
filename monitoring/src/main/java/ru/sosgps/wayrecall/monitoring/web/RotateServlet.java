package ru.sosgps.wayrecall.monitoring.web;

import org.deegree.services.wms.controller.WMSController;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 20.11.12
 * Time: 16:47
 * To change this template use File | Settings | File Templates.
 */
@WebServlet(name = "RotateServlet", urlPatterns = "rotate")
public class RotateServlet extends HttpServlet {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RotateServlet.class);

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        URL path = getURL(request);

        log.trace("requested path:"+path);

        String r = request.getParameter("rot");
        if (r == null) {
            r = "0";
        }
        double rot = 0.;
        try {
            rot = Double.parseDouble(r);
        } catch (NumberFormatException e) {
        }


        String file = path.getFile();
        if(file == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setDateHeader("Last-Modified", file != null? new File(file).lastModified() : 0);
        response.setDateHeader("Expires", System.currentTimeMillis() + 1000 * 60 * 60 * 24);

        BufferedImage image = ImageIO.read(path);
        AffineTransform tx = new AffineTransform();

        tx.rotate(Math.toRadians(rot), image.getWidth() / 2, image.getHeight() / 2);

        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        image = op.filter(image, null);


        ServletContext sc = getServletContext();

        // Get the MIME type of the image
        String mimeType = sc.getMimeType(path.toExternalForm());
        if (mimeType == null) {
            sc.log("Could not get MIME type of " + path);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }


        // Set content type
        response.setContentType(mimeType);

        Iterator<ImageWriter> iter = ImageIO.getImageWritersByMIMEType(mimeType);
        ImageWriter imwriter = iter.next();

        try (ServletOutputStream out = response.getOutputStream();
             ImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(out)
        ) {
            imwriter.setOutput(imageOutputStream);
            try {
                imwriter.write(image);
            } finally {
                imwriter.dispose();
                imageOutputStream.flush();
            }
        }
    }

    private String getPath(HttpServletRequest request) {
        String img = (String) request.getParameter("img");
        return getServletContext().getRealPath("images/" + img);
    }

    private URL getURL(HttpServletRequest request) {
        String img = (String) request.getParameter("img");
        try {
            return getServletContext().getResource("/images/" + img);
        } catch (MalformedURLException e) {
           throw  new RuntimeException(e);
        }
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {

            String requestedFileName = getPath(req);

            if (requestedFileName != null)
            {
                File file = new File(requestedFileName);
                long l = file.lastModified();
                return l != 0L ? l : super.getLastModified(req);
            }

            return super.getLastModified(req);
    }

}
