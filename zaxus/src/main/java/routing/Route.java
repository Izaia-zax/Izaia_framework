package main.java.routing;

import java.lang.reflect.Method;

import main.java.annotations.HttpMethod;

/** Un couple (URL + methode HTTP) associe a une methode de controleur. */
public class Route {

    private final String url;
    private final HttpMethod httpMethod;
    private final Class<?> controllerClass;
    private final Method method;

    public Route(String url, HttpMethod httpMethod, Class<?> controllerClass, Method method) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return httpMethod + " " + url + " -> " + controllerClass.getName() + "." + method.getName() + "()";
    }
}
