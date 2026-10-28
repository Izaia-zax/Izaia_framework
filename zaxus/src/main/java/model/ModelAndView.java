package main.java.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resultat d'un controleur : la vue a afficher et les donnees a lui transmettre (Sprint-5). */
public class ModelAndView {

    private String view;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public ModelAndView() {
    }

    public ModelAndView(String view) {
        this.view = view;
    }

    public ModelAndView(String view, Map<String, Object> data) {
        this.view = view;
        if (data != null) {
            this.data.putAll(data);
        }
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    /** Ajoute une donnee ; retourne this pour chainer les appels. */
    public ModelAndView addObject(String name, Object value) {
        data.put(name, value);
        return this;
    }
}
