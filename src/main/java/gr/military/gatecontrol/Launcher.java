package gr.military.gatecontrol;

/**
 * Top-level launcher class used by jpackage.
 *
 * Spring Boot repackages classes into BOOT-INF/classes/ which the standard
 * classloader cannot see. This class stays at the JAR root (visible to
 * jpackage) and delegates to PropertiesLauncher which handles the nested
 * class loading. Requires <layout>ZIP</layout> in spring-boot-maven-plugin.
 */
public class Launcher {

    public static void main(String[] args) throws Exception {
        Class<?> launcher = Class.forName("org.springframework.boot.loader.launch.PropertiesLauncher");
        java.lang.reflect.Method main = launcher.getMethod("main", String[].class);
        main.invoke(null, (Object) args);
    }
}