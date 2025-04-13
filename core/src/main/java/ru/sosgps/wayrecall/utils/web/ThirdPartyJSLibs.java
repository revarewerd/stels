/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.utils.web;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author nickl
 */
public class ThirdPartyJSLibs extends HttpServlet {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThirdPartyJSLibs.class);

    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

    private String filePath = null;
    private boolean filePathExists = false;

    @Override
    public void init() throws ServletException {
        super.init();
        filePath = System.getenv("ThirdPartyJS");
        if (filePath == null) {
            filePath = this.getInitParameter("folderPath");
        }

        filePathExists = filePath != null && new File(filePath).exists();
    }

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);

        String fullFileName = getRequestedFileName(request);

        if (fullFileName == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
            return;
        }

        InputStream fileInputStream = null;

        if (gzipSupported(request)) {
            fileInputStream = getInputStream(response, fullFileName + ".gz");
            if (fileInputStream != null) {
                log.debug("processing " + fullFileName + " as gz");
                response.setHeader("Content-Encoding", "gzip");
            }
        }

        if (fileInputStream == null) {
            fileInputStream = getInputStream(response, fullFileName);
        }

        if (fileInputStream == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
            return;
        }

        response.setContentType(getFileContentType(fullFileName));
        writeStreamToResponse(response, fileInputStream);


    }

    private String getRequestedFileName(HttpServletRequest request) throws UnsupportedEncodingException {
        String requestedFile = request.getPathInfo();
        if (requestedFile == null) {
            return null;
        }
        return request.getServletPath() + URLDecoder.decode(requestedFile, "UTF-8");
    }

    private boolean gzipSupported(HttpServletRequest request) {
        boolean gzip = false;

        String accept = request.getHeader("Accept-Encoding");
        if (accept != null && accept.indexOf("gzip") >= 0)
            gzip = true;
        return gzip;
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        try {
            String requestedFileName = getRequestedFileName(req);

            if (requestedFileName == null)
                return super.getLastModified(req);

            try {
                URL resource = getServletContext().getResource(requestedFileName);
                if (resource != null) {
                    URLConnection conn = resource.openConnection();
                    return conn.getLastModified();
                }
            } catch (Exception e) {
                log.warn("err", e);
            }

            if (!filePathExists)
                return super.getLastModified(req);

            File file = new File(filePath, requestedFileName);
            if (!file.exists()) {
                return super.getLastModified(req);
            }

            return file.lastModified();

        } catch (UnsupportedEncodingException e) {
            log.warn("getLastModified error", e);
            return super.getLastModified(req);
        }
    }

    private InputStream getInputStream(HttpServletResponse response, String fullFileName) throws FileNotFoundException {
        InputStream fileInputStream = null;

        fileInputStream = getServletContext().getResourceAsStream(fullFileName);

        if (fileInputStream != null) {
            log.debug("loading from getResourceAsStream" + fullFileName);
            try {
                URL resource = getServletContext().getResource(fullFileName);
                if (resource != null) {
                    URLConnection conn = resource.openConnection();
                    response.setHeader("Content-Length", String.valueOf(conn.getContentLength()));
                    response.setDateHeader("Last-Modified", conn.getLastModified());
                    response.setDateHeader("Expires", System.currentTimeMillis() + 1000 * 60 * 60 * 24);
                }
            } catch (Exception e) {
                log.debug("errr", e);
            }
        }

        if (filePathExists && fileInputStream == null) {

            File file = new File(filePath, fullFileName);

            if (!file.exists()) {
                return null;
            }

            response.setHeader("Content-Length", String.valueOf(file.length()));
            response.setDateHeader("Last-Modified", file.lastModified());
            response.setDateHeader("Expires", System.currentTimeMillis() + 1000 * 60 * 60 * 24);

            fileInputStream = new FileInputStream(file);

        }

        return fileInputStream;
    }

    private String getFileContentType(String fullFileName) {
        // Get content type by filename.
        String contentType = getServletContext().getMimeType(fullFileName);

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return contentType;
    }

    private void writeStreamToResponse(HttpServletResponse response, InputStream fileInputStream) throws IOException {
        if (fileInputStream == null) {
            throw new NullPointerException("fileInputStream is null, filePath=" + filePath);
        }

        // Prepare streams.
        BufferedInputStream input = null;
        BufferedOutputStream output = null;

        try {
            // Open streams.
            input = new BufferedInputStream(fileInputStream, DEFAULT_BUFFER_SIZE);
            output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

            // Write file contents to response.
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } finally {
            // Gently close streams.
            close(output);
            close(input);
        }
    }

    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                // Do your thing with the exception. Print it, log it or mail it.
                e.printStackTrace();
            }
        }
    }


    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
