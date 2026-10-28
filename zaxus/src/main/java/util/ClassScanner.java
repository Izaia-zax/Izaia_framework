package main.java.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utilitaire generique de parcours du classpath (Sprint-1).
 * Fonctionne pour les classes en dossier (WEB-INF/classes) comme pour celles dans un jar.
 */
public final class ClassScanner {

    private ClassScanner() {
    }

    /** Retourne les classes d'un package (sous-packages inclus) portant l'annotation donnee. */
    public static List<Class<?>> findAnnotatedClasses(String packageName, Class<? extends Annotation> annotation)
            throws IOException, ClassNotFoundException {
        ClassLoader loader = classLoader();
        List<Class<?>> result = new ArrayList<>();
        for (String className : findClassNames(packageName)) {
            Class<?> clazz = Class.forName(className, false, loader);
            if (clazz.isAnnotationPresent(annotation)) {
                result.add(clazz);
            }
        }
        return result;
    }

    /** Retourne les noms (tries) de toutes les classes d'un package, sous-packages inclus. */
    public static List<String> findClassNames(String packageName) throws IOException {
        String path = packageName.replace('.', '/');
        TreeSet<String> names = new TreeSet<>();
        Enumeration<URL> resources = classLoader().getResources(path);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try {
                if ("file".equals(url.getProtocol())) {
                    scanDirectory(new File(url.toURI()), packageName, names);
                } else if ("jar".equals(url.getProtocol())) {
                    scanJar(url, path, names);
                }
            } catch (URISyntaxException e) {
                throw new IOException("URL de package invalide : " + url, e);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    private static void scanDirectory(File dir, String packageName, TreeSet<String> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + name, out);
            } else if (isClassFile(name)) {
                out.add(packageName + "." + name.substring(0, name.length() - ".class".length()));
            }
        }
    }

    private static void scanJar(URL url, String path, TreeSet<String> out) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (entry.startsWith(path + "/") && isClassFile(entry)) {
                    out.add(entry.substring(0, entry.length() - ".class".length()).replace('/', '.'));
                }
            }
        }
    }

    private static boolean isClassFile(String name) {
        return name.endsWith(".class")
                && !name.endsWith("package-info.class")
                && !name.endsWith("module-info.class");
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : ClassScanner.class.getClassLoader();
    }
}
