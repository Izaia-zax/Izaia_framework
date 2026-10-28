package main.java.routing;

/** Levee au demarrage quand deux methodes declarent la meme URL avec la meme methode HTTP. */
public class DuplicateRouteException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public DuplicateRouteException(Route existing, Route duplicate) {
        super("Route en double : " + duplicate.getHttpMethod() + " " + duplicate.getUrl()
                + " est declaree par " + existing.getControllerClass().getName() + "." + existing.getMethod().getName()
                + "() et par " + duplicate.getControllerClass().getName() + "." + duplicate.getMethod().getName() + "()");
    }
}
