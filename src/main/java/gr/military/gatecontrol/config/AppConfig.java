package gr.military.gatecontrol.config;

/** Runtime configuration loaded from db.properties at startup. */
public class AppConfig {

    private static String gateName = "Main Gate";

    public static void setGateName(String name) {
        if (name != null && !name.isBlank()) gateName = name.trim();
    }

    public static String getGateName() {
        return gateName;
    }
}
