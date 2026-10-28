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
        ServletContext context = getServletContext();
        if (context.getAttribute(FrontControllerListener.ROUTES_ATTRIBUTE) != null) {
            return; // deja rempli au demarrage par FrontControllerListener (Sprint-4)
        }
        // Secours : pas de listener declare, on construit la table ici.
        String packageName = getInitParameter(FrontControllerListener.PACKAGE_PARAM);
        if (packageName == null || packageName.trim().isEmpty()) {
            packageName = context.getInitParameter(FrontControllerListener.PACKAGE_PARAM);
        }
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new ServletException("Le parametre '" + FrontControllerListener.PACKAGE_PARAM + "' est manquant dans web.xml.");
        }
        try {
            context.setAttribute(FrontControllerListener.ROUTES_ATTRIBUTE, RouteRegistry.build(packageName.trim()));
        } catch (RuntimeException e) {
            throw new ServletException("Echec de la construction des routes : " + e.getMessage(), e);
        }
    }

    private void render(HttpServletRequest req, HttpServletResponse res, Object result) throws ServletException, IOException {
        if (result == null) {
            return;
        }
        if (result instanceof ModelAndView) {
            ModelAndView mv = (ModelAndView) result;
            if (mv.getView() == null || mv.getView().trim().isEmpty()) {
                writeHtml(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "<p>Le ModelAndView ne contient aucune vue.</p>");
                return;
            }
            for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }
            String view = mv.getView().startsWith("/") ? mv.getView() : "/" + mv.getView();
            RequestDispatcher dispatcher = req.getRequestDispatcher(view);
            if (dispatcher == null) {
                writeHtml(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "<p>Vue introuvable : " + Util.escapeHtml(view) + "</p>");
                return;
            }
            dispatcher.forward(req, res);
            return;
        }
        // Sprint-3-bis : tout autre resultat est affiche tel quel
        res.setContentType("text/plain;charset=UTF-8");
        try (PrintWriter out = res.getWriter()) {
            out.print(String.valueOf(result));
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
        RouteRegistry routes = (RouteRegistry) getServletContext().getAttribute(FrontControllerListener.ROUTES_ATTRIBUTE);
        if (routes == null) {
            writeHtml(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "<p>Erreur interne du framework : table des routes non initialisee.</p>");
            return;
        }

        String path = extractPath(req);
        HttpMethod httpMethod = HttpMethod.fromString(req.getMethod());
        Route route = httpMethod == null ? null : routes.find(path, httpMethod);

        if (route == null) {
            Set<HttpMethod> allowed = routes.allowedMethods(path);
            if (!allowed.isEmpty()) {
                res.setHeader("Allow", String.join(", ", allowed.stream().map(Enum::name).toArray(String[]::new)));
                writeHtml(res, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "<p>Methode " + Util.escapeHtml(req.getMethod()) + " non autorisee pour l'URL <b>"
                                + Util.escapeHtml(path) + "</b>. Methodes autorisees : " + Util.escapeHtml(allowed) + ".</p>");
            } else {
                writeHtml(res, HttpServletResponse.SC_NOT_FOUND, notFoundBody(path, routes));
            }
            return;
        }

        Object result;
        try {
            Object controller = route.getControllerClass().getDeclaredConstructor().newInstance();
            result = route.getMethod().invoke(controller, resolveArguments(route.getMethod()));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log("Erreur dans " + route, cause);
            writeHtml(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorBody(route, cause));
            return;
        } catch (ReflectiveOperationException | RuntimeException e) {
            log("Impossible d'executer " + route, e);
            writeHtml(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorBody(route, e));
            return;
        }

        render(req, res, result);
    }

     private static String notFoundBody(String path, RouteRegistry routes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>URL inconnue : <b>").append(Util.escapeHtml(path)).append("</b></p>");
        sb.append("<h3>URLs valides :</h3>");
        if (routes.size() == 0) {
            sb.append("<p>Aucune URL enregistree.</p>");
        } else {
            sb.append("<ul>");
            for (Route route : routes.all()) {
                sb.append("<li>").append(Util.escapeHtml(route.getHttpMethod())).append(' ')
                        .append(Util.escapeHtml(route.getUrl())).append(" &rarr; ")
                        .append(Util.escapeHtml(route.getControllerClass().getName())).append('.')
                        .append(Util.escapeHtml(route.getMethod().getName())).append("()</li>");
            }
            sb.append("</ul>");
        }
        return sb.toString();
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
