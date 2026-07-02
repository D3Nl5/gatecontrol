package gr.military.gatecontrol.config;

import gr.military.gatecontrol.auth.AdminAuth;
import gr.military.gatecontrol.auth.CredentialCrypto;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Loads database connection settings from db.properties.
 *
 * Password handling:
 *   db.password.enc=ENC:...  — AES-256-GCM encrypted (preferred)
 *   db.password=plaintext    — plain text fallback (dev / first run)
 *
 * To encrypt the password, run CredentialCrypto.main() once and
 * replace db.password with db.password.enc in db.properties.
 *
 * Installed path : <install-dir>/app/db.properties
 * Dev fallback   : src/main/resources/db.properties  (classpath)
 */
@Configuration
@Profile("mssql")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        Properties props = loadDbProperties();

        String url      = ensureUnicodeParams(props.getProperty("db.url"));
        String username = props.getProperty("db.username");
        String password = resolvePassword(props);

        // Load admin password hash
        AdminAuth.setHashFromProperties(props.getProperty("admin.password.hash"));

        // Load optional gate name (default: "Main Gate")
        AppConfig.setGateName(props.getProperty("gate.name", "Main Gate"));

        System.out.println("Connecting with URL: " + url);

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    /**
     * Resolves the database password.
     *
     * Priority:
     *  1. db.password.enc=ENC:...  — AES-256-GCM, machine-fingerprint key (production)
     *  2. db.password=<plain>      — plain text written by the installer on first install;
     *                                auto-upgraded to db.password.enc on the first successful run.
     */
    private String resolvePassword(Properties props) {
        // ── 1. Encrypted form ──────────────────────────────────────────────────
        String enc = props.getProperty("db.password.enc");
        if (enc != null && !enc.isBlank()) {
            try {
                return CredentialCrypto.decrypt(enc);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to decrypt db.password.enc. " +
                        "If this machine's hardware changed, re-run the installer to reconfigure.",
                        e);
            }
        }

        // ── 2. Plain text (installer-written) — auto-upgrade ──────────────────
        String plain = props.getProperty("db.password");
        if (plain != null) {
            tryUpgradePassword(plain);
            return plain;
        }

        return null;
    }

    /**
     * Encrypts the plain-text password and rewrites db.properties so that
     * db.password=<plain> is replaced by db.password.enc=ENC:<encrypted>.
     * Silently skips if the file is not writable (e.g. first-time read-only mount).
     */
    private void tryUpgradePassword(String plainPassword) {
        Path propsPath = resolveExternalPropertiesPath();
        if (!propsPath.toFile().exists()) return;
        // Note: we do NOT skip on canWrite()==false — File.canWrite() is unreliable
        // with Windows ACLs (may return false even when icacls has granted access).
        // Let the actual write attempt below decide; the catch handles it gracefully.

        try {
            String encrypted = CredentialCrypto.encrypt(plainPassword);

            List<String> lines   = Files.readAllLines(propsPath, StandardCharsets.UTF_8);
            List<String> updated = new ArrayList<>();
            boolean upgraded = false;

            for (String line : lines) {
                // Replace the plain-text key; skip if it's actually the .enc key
                if (line.startsWith("db.password=") && !line.startsWith("db.password.enc=")) {
                    updated.add("db.password.enc=" + encrypted);
                    upgraded = true;
                    // do NOT add the plain-text line — it is intentionally dropped
                } else {
                    updated.add(line);
                }
            }

            if (upgraded) {
                Files.write(propsPath, updated, StandardCharsets.UTF_8);
                System.out.println("db.password encrypted and saved as db.password.enc");
            }
        } catch (Exception e) {
            // Non-fatal: the app still connects; the upgrade will be retried on the next start.
            System.err.println("Warning: could not auto-encrypt db.password: " + e.getMessage());
        }
    }

    private String ensureUnicodeParams(String url) {
        if (url == null) return null;
        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("sendStringParametersAsUnicode")) {
            sb.append(";sendStringParametersAsUnicode=true");
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Properties loading
    // ─────────────────────────────────────────────────────────────────────────

    private Properties loadDbProperties() {
        Path externalPath = resolveExternalPropertiesPath();

        if (externalPath.toFile().exists()) {
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(externalPath.toFile()), StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.load(reader);
                System.out.println("Loaded DB config from: " + externalPath);
                return props;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read db.properties at " + externalPath, e);
            }
        }

        // Dev fallback: classpath
        InputStream classpathStream = getClass().getClassLoader().getResourceAsStream("db.properties");
        if (classpathStream != null) {
            try (Reader reader = new InputStreamReader(classpathStream, StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.load(reader);
                System.out.println("Loaded DB config from classpath db.properties (dev fallback)");
                return props;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read classpath db.properties", e);
            }
        }

        throw new RuntimeException(
                "db.properties not found at " + externalPath + " and not on classpath. " +
                        "Run the installer to configure the database connection.");
    }

    private Path resolveExternalPropertiesPath() {
        // 1. Explicit path injected by the jpackage launcher via --java-options.
        //    "$APPDIR" in jpackage expands to the app/ directory at runtime,
        //    so this resolves to <install-dir>/app/db.properties.
        String explicit = System.getProperty("app.db.properties");
        if (explicit != null) {
            return Paths.get(explicit);
        }

        // 2. Derive from the code source URL.
        //    Spring Boot 3.x LaunchedClassLoader reports a "nested:" URL:
        //      nested:/path/to/app.jar!/BOOT-INF/classes/
        //    Standard java -jar reports:
        //      jar:file:/path/to/app.jar!/BOOT-INF/classes/
        //    Plain class directory (IDE):
        //      file:/path/to/classes/
        //    Strip the outer scheme and the inner "!/..." suffix to get the JAR file.
        try {
            String loc = DatabaseConfig.class
                    .getProtectionDomain().getCodeSource().getLocation().toString();
            if (loc.contains("!/")) {
                loc = loc.replaceFirst("^(jar:|nested:)", "").split("!")[0];
                if (!loc.startsWith("file:")) loc = "file:" + loc;
            }
            Path p = Paths.get(new java.net.URI(loc));
            return (p.toFile().isDirectory() ? p : p.getParent()).resolve("db.properties");
        } catch (Exception ignored) {
            // fall through
        }

        // 3. Last resort for jpackage layout: jpackage sets user.dir to the
        //    installation root (where the .exe lives); db.properties is one
        //    level deeper in the app/ sub-directory.
        Path appSubDir = Paths.get(System.getProperty("user.dir"), "app", "db.properties");
        if (appSubDir.toFile().exists()) return appSubDir;
        return Paths.get(System.getProperty("user.dir"), "db.properties");
    }
}