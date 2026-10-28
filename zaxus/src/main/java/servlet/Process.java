package main.java.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import main.java.annotations.HttpMethod;
import main.java.model.ModelAndView;
import main.java.routing.Route;
import main.java.routing.RouteRegistry;
import main.java.util.Util;

public class Process extends HttpServlet {

    private static final String SPRING_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

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

   
    private Object[] resolveArguments(Method method) {
        Object[] args = new Object[method.getParameterCount()];
        if (args.length == 0) {
            return args;
        }
        Class<?> springType = Util.loadClassOrNull(SPRING_CONTEXT_CLASS);
        boolean wantsSpring = springType != null && Util.haveParameter(method, springType);
        Object springContext = getServletContext().getAttribute(FrontControllerListener.SPRING_CONTEXT_ATTRIBUTE);
        if (wantsSpring && springContext == null) {
            throw new IllegalStateException("Pas de springcontext : la methode " + method.getName()
                    + "() attend un WebApplicationContext mais aucun contexte Spring n'est disponible.");
        }
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (springType != null && types[i].equals(springType)) {
                args[i] = springContext;
            } else {
                throw new IllegalStateException("Parametre non supporte (" + types[i].getName() + ") dans "
                        + method.getDeclaringClass().getName() + "." + method.getName() + "().");
            }
        }
        return args;
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
        res.setContentType("text/plain;charset=UTF-8");
        try (PrintWriter out = res.getWriter()) {
            out.print(String.valueOf(result));
        }
    }

    private static String extractPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = (contextPath != null && uri.startsWith(contextPath)) ? uri.substring(contextPath.length()) : uri;
        int semicolon = path.indexOf(';');
        if (semicolon >= 0) {
            path = path.substring(0, semicolon);
        }
        if (path.isEmpty()) {
            path = "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
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

    private static String errorBody(Route route, Throwable cause) {
        return "<p>Erreur pendant l'execution de <b>" + Util.escapeHtml(route) + "</b></p><pre>"
                + Util.escapeHtml(cause) + "</pre>";
    }

    private static void writeHtml(HttpServletResponse res, int status, String body) throws IOException {
        res.setStatus(status);
        res.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = res.getWriter()) {
            out.println("<!DOCTYPE html><html><body>");
            out.println(body);
            out.println("</body></html>");
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
