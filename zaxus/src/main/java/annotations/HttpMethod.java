package main.java.annotations;

/** Methodes HTTP gerees par le framework (Sprint-3). */
public enum HttpMethod {
    GET, POST;

    /** Retourne la methode correspondante, ou null si elle n'est pas geree. */
    public static HttpMethod fromString(String name) {
        if (name == null) {
            return null;
        }
        for (HttpMethod m : values()) {
            if (m.name().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }
}
