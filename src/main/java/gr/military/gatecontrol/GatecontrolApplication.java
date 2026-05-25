package gr.military.gatecontrol;

import gr.military.gatecontrol.ui.GateUI;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@SpringBootApplication
public class GatecontrolApplication {

    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext context =
                    SpringApplication.run(GatecontrolApplication.class, args);
            GateUI.setContext(context);
            Application.launch(GateUI.class, args);

        } catch (Exception e) {
            handleStartupFailure(e, args);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Startup failure handling
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleStartupFailure(Throwable error, String[] args) {
        writeErrorLog(error);

        StartupErrorApp.errorSummary = buildSummary(error);
        StartupErrorApp.errorDetail  = buildDetail(error);

        try {
            Application.launch(StartupErrorApp.class, args);
        } catch (Exception launchEx) {
            // JavaFX itself failed — last resort: stderr only
            System.err.println("=== GateControl failed to start ===");
            error.printStackTrace();
        }
        System.exit(1);
    }

    /**
     * Translates common root-cause messages into plain Greek.
     */
    private static String buildSummary(Throwable error) {
        Throwable root = rootCause(error);
        String msg = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();

        if (msg.contains("Login failed") || msg.contains("password") || msg.contains("18456"))
            return "Αποτυχία σύνδεσης στη βάση δεδομένων.\n\n" +
                   "Λάθος όνομα χρήστη ή κωδικός SQL Server.\n" +
                   "Ελέγξτε τα στοιχεία στο db.properties και τις ρυθμίσεις\n" +
                   "του SQL Server (Mixed Authentication, SA login enabled).";

        if (msg.contains("Cannot open database") || msg.contains("does not exist"))
            return "Αποτυχία σύνδεσης στη βάση δεδομένων.\n\n" +
                   "Η βάση δεδομένων δεν βρέθηκε.\n" +
                   "Ελέγξτε το όνομα της βάσης στο db.properties.";

        if (msg.contains("Connection refused") || msg.contains("Unable to connect")
                || msg.contains("TCP Provider") || msg.contains("Network"))
            return "Αποτυχία σύνδεσης στη βάση δεδομένων.\n\n" +
                   "Ο SQL Server δεν είναι προσβάσιμος.\n" +
                   "Βεβαιωθείτε ότι ο SQL Server τρέχει και\n" +
                   "ο host/port στο db.properties είναι σωστός.";

        if (msg.contains("decrypt") || msg.contains("AES") || msg.contains("GCM")
                || msg.contains("db.password.enc"))
            return "Αποτυχία αποκρυπτογράφησης του κωδικού βάσης.\n\n" +
                   "Αν άλλαξε το hardware αυτής της μηχανής,\n" +
                   "επανεκτελέστε τον εγκαταστάτη.";

        if (msg.contains("db.properties"))
            return "Δεν βρέθηκε το αρχείο ρυθμίσεων db.properties.\n\n" +
                   "Επανεκτελέστε τον εγκαταστάτη για να το δημιουργήσετε.";

        return "Απρόβλεπτο σφάλμα κατά την εκκίνηση.\n\n" + msg;
    }

    /**
     * Builds a full stack-trace string (root cause chain, max 6 levels).
     */
    private static String buildDetail(Throwable error) {
        StringBuilder sb = new StringBuilder();
        Throwable t = error;
        int depth = 0;
        while (t != null && depth < 6) {
            if (depth > 0) sb.append("\n── Caused by ──────────────────────────────────────\n");
            sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");
            for (StackTraceElement el : t.getStackTrace()) {
                sb.append("  at ").append(el).append("\n");
            }
            t = t.getCause();
            depth++;
        }
        return sb.toString();
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }

    /**
     * Writes gatecontrol-error.log next to db.properties (or in user.dir/app/).
     */
    private static void writeErrorLog(Throwable error) {
        try {
            String dbProp = System.getProperty("app.db.properties");
            Path dir = dbProp != null
                    ? Paths.get(dbProp).getParent()
                    : Paths.get(System.getProperty("user.dir"), "app");

            if (dir != null && !dir.toFile().exists()) dir.toFile().mkdirs();

            Path logFile = (dir != null ? dir : Paths.get(System.getProperty("user.dir")))
                    .resolve("gatecontrol-error.log");

            String content = "=== GateControl Startup Error ===\n"
                    + LocalDateTime.now() + "\n\n"
                    + buildDetail(error);

            Files.writeString(logFile, content, StandardCharsets.UTF_8);
            System.err.println("Error log: " + logFile);
        } catch (IOException ignored) {
            // best-effort only
        }
    }
}
