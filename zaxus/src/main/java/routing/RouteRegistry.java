package main.java.routing;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import main.java.annotations.Controller;
import main.java.annotations.HttpMethod;
import main.java.annotations.Url;
import main.java.util.ClassScanner;

/**
 * Table de routage : (URL + methode HTTP) -> (controleur, methode).
 * Une meme URL peut exister en GET et en POST, mais pas deux fois avec la meme methode.
 */
public class RouteRegistry {

    private final Map<String, Route> routes = new TreeMap<>();

    private static String key(String url, HttpMethod httpMethod) {
        return url + " " + httpMethod.name();
    }

    /** Enregistre une route ; leve DuplicateRouteException si (URL + methode HTTP) existe deja. */
    public void register(Route route) {
        String k = key(route.getUrl(), route.getHttpMethod());
        Route existing = routes.get(k);
        if (existing != null) {
            throw new DuplicateRouteException(existing, route);
        }
        routes.put(k, route);
    }

    public Route find(String url, HttpMethod httpMethod) {
        return routes.get(key(url, httpMethod));
    }

    /** Methodes HTTP declarees pour cette URL (vide si l'URL est inconnue). */
    public Set<HttpMethod> allowedMethods(String url) {
        Set<HttpMethod> allowed = EnumSet.noneOf(HttpMethod.class);
        for (HttpMethod m : HttpMethod.values()) {
            if (routes.containsKey(key(url, m))) {
                allowed.add(m);
            }
        }
        return allowed;
    }

    public Collection<Route> all() {
        return Collections.unmodifiableCollection(routes.values());
    }

    public int size() {
        return routes.size();
    }

    /** Scanne un package, repere les @Controller puis leurs methodes @Url et construit la table. */
    public static RouteRegistry build(String packageName) {
        RouteRegistry registry = new RouteRegistry();
        List<Class<?>> controllers;
        try {
            controllers = ClassScanner.findAnnotatedClasses(packageName, Controller.class);
        } catch (Exception e) {
            throw new IllegalStateException("Echec du scan des controllers dans le package '" + packageName + "'.", e);
        }

        for (Class<?> controller : controllers) {
            checkInstantiable(controller);
            List<Method> methods = new ArrayList<>(List.of(controller.getDeclaredMethods()));
            methods.sort((a, b) -> a.getName().compareTo(b.getName()));
            for (Method method : methods) {
                Url url = method.getAnnotation(Url.class);
                if (url == null) {
                    continue;
                }
                if (!url.value().startsWith("/")) {
                    throw new IllegalStateException("L'URL '" + url.value() + "' de "
                            + controller.getName() + "." + method.getName() + "() doit commencer par '/'.");
                }
                method.setAccessible(true);
                registry.register(new Route(url.value(), url.method(), controller, method));
            }
        }
        return registry;
    }

    private static void checkInstantiable(Class<?> controller) {
        if (controller.isInterface() || Modifier.isAbstract(controller.getModifiers())) {
            throw new IllegalStateException("Le controller " + controller.getName() + " ne peut pas etre instancie (abstrait).");
        }
        try {
            controller.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Le controller " + controller.getName() + " doit avoir un constructeur sans argument.", e);
        }
    }
}
