package main.java.util;

import java.lang.reflect.Method;

public final class Util {

    private Util() {
    }
public static String escapeHtml(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
     public static boolean haveParameter(Method methode, Class<?> param) {
        if (methode == null || param == null) {
            return false;
        }
        for (Class<?> type : methode.getParameterTypes()) {
            if (type.equals(param)) {
                return true;
            }
        }
        return false;
    }

    public static Class<?> loadClassOrNull(String name) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName(name, false, loader != null ? loader : Util.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

}