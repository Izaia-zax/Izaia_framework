package main.java.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de marquage utilisée pour identifier les classes "Controller"
 * du projet qui utilise le framework zaxus.
 *
 * Elle ne fait rien par elle-même : c'est juste une étiquette posée sur une
 * classe. C'est le FrontController (Process) qui, au démarrage, scanne le
 * package des controllers et garde la liste des classes qui portent cette
 * annotation.
 *
 * @Retention(RUNTIME) est indispensable : sans ça, l'annotation disparaît
 * du bytecode et ne serait plus visible via la réflexion (Class.getAnnotation)
 * au moment du scan.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
}
