package main.java.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associe une methode de controleur a une URL (Sprint-2) et a une methode HTTP (Sprint-3).
 * Exemple : @Url("/home") ou @Url(value = "/form", method = HttpMethod.POST)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Url {
    /** URL geree, relative au contexte de l'application, ex : "/home". */
    String value();

    /** Methode HTTP geree (GET par defaut). */
    HttpMethod method() default HttpMethod.GET;
}
