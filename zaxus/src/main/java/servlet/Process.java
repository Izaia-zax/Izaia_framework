package main.java.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import main.java.annotations.Controller;

public class Process extends HttpServlet {

    private String controllersPackage;

    private List<String> listControllers;

    @Override
    public void init() throws ServletException {
        controllersPackage = getInitParameter("controller_package_name");
        if (controllersPackage == null || controllersPackage.trim().isEmpty()) {
            throw new ServletException("Le parametre d'initialisation 'controller_package_name' est manquant dans web.xml.");
        }
        try {
            listControllers = scanControllers(controllersPackage);
        } catch (Exception e) {
            throw new ServletException("Echec du scan des controllers dans le package '" + controllersPackage + "'.", e);
        }
    }


    private List<String> scanControllers(String packageName) throws Exception {
        List<String> found = new ArrayList<>();

        URL resource = Thread.currentThread().getContextClassLoader().getResource(packageName.replace('.', '/'));
        if (resource == null) {
            return found;
        }

        File folder = new File(resource.getFile());
        File[] files = folder.listFiles();
        if (files == null) {
            return found;
        }

        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                found.addAll(scanControllers(packageName + "." + name));
            } else if (name.endsWith(".class")) {
                Class<?> clazz = Class.forName(packageName + "." + name.substring(0, name.length() - 6));
                if (clazz.isAnnotationPresent(Controller.class)) {
                    found.add(clazz.getName());
                }
            }
        }

        return found;
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StringBuilder url = new StringBuilder(req.getRequestURL());
        String queryString = req.getQueryString();
        if (queryString != null) {
            url.append('?').append(queryString);
        }

        res.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = res.getWriter()) {
            out.println("<!DOCTYPE html><html><body>");
            out.println("<p>URL demandee : " + url + "</p>");
            out.println("<h3>Controllers trouves dans le package \"" + controllersPackage + "\" :</h3>");
            out.println("<ul>");
            for (String controller : listControllers) {
                out.println("<li>" + controller + "</li>");
            }
            out.println("</ul>");
            out.println("</body></html>");
        } catch (Exception e) {
            e.printStackTrace();
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne du framework.");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
}
