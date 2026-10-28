package main.java.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import main.java.routing.RouteRegistry;

/**
 * Ecoute le demarrage de l'application (Sprint-4) : scanne les controllers, remplit la table
 * des URLs et la place dans le ServletContext. Expose aussi le contexte Spring (Sprint 5-bis).
 *
 * A declarer dans web.xml APRES le ContextLoaderListener de Spring, pour que le contexte
 * Spring racine existe deja quand ce listener s'execute.
 */
public class FrontControllerListener implements ServletContextListener {

    public static final String SPRING_ROOT = "org.springframework.web.context.WebApplicationContext.ROOT";
    public static final String SPRING_CONTEXT_ATTRIBUTE = "springContext";
    public static final String ROUTES_ATTRIBUTE = "zaxus.routes";
    public static final String PACKAGE_PARAM = "controller_package_name";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();

        // Sprint 5-bis : republier le contexte Spring racine sous le nom "springContext" (null s'il n'y a pas Spring)
        context.setAttribute(SPRING_CONTEXT_ATTRIBUTE, context.getAttribute(SPRING_ROOT));

        String packageName = context.getInitParameter(PACKAGE_PARAM);
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalStateException("Le parametre '" + PACKAGE_PARAM + "' (context-param) est manquant dans web.xml.");
        }

        // Une route en double leve DuplicateRouteException ici : l'application refuse de demarrer.
        RouteRegistry registry = RouteRegistry.build(packageName.trim());
        context.setAttribute(ROUTES_ATTRIBUTE, registry);
        context.log("[Zaxus] " + registry.size() + " route(s) enregistree(s) depuis '" + packageName + "'.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(ROUTES_ATTRIBUTE);
    }
}
