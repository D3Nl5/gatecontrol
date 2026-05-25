package gr.military.gatecontrol.auth;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption for the database password stored in db.properties.
 *
 * The encryption key is derived from the machine's hardware identifiers
 * (motherboard serial + CPU ID) so the encrypted value only decrypts on
 * the machine it was encrypted on.
 *
 * db.properties format:
 *   db.password.enc=<base64-encrypted-value>   ← encrypted (used in production)
 *   db.password=                               ← leave blank or remove
 *
 * To encrypt a password for the first time, run the main() method
 * of this class or use the installer's "Encrypt" button.
 */
public class CredentialCrypto {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN = 12;
    private static final int    GCM_TAG    = 128;
    private static final String PREFIX     = "ENC:";

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encrypts plaintext and returns PREFIX + base64(iv + ciphertext).
     */
    public static String encrypt(String plaintext) throws Exception {
        byte[] key = deriveKey();
        byte[] iv  = generateIv();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, toSecretKey(key), new GCMParameterSpec(GCM_TAG, iv));
        byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Prepend IV so we can recover it during decryption
        byte[] combined = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

        return PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts a value produced by encrypt().
     * If the value does not start with PREFIX it is returned as-is
     * (plain-text fallback for dev / first-run).
     */
    public static String decrypt(String value) throws Exception {
        if (value == null || value.isBlank()) return value;
        if (!value.startsWith(PREFIX))         return value; // plain-text fallback

        byte[] combined = Base64.getDecoder().decode(value.substring(PREFIX.length()));
        byte[] iv          = Arrays.copyOfRange(combined, 0, GCM_IV_LEN);
        byte[] cipherBytes = Arrays.copyOfRange(combined, GCM_IV_LEN, combined.length);

        byte[] key = deriveKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, toSecretKey(key), new GCMParameterSpec(GCM_TAG, iv));
        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }

    /**
     * Returns true if the value is already encrypted (starts with PREFIX).
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key derivation — uses machine hardware fingerprint
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Derives a 256-bit AES key from machine hardware identifiers.
     * Uses motherboard serial number (via WMIC) and falls back to
     * hostname + OS name if WMIC is unavailable (non-Windows or no permission).
     */
    static byte[] deriveKey() throws Exception {
        String fingerprint = getMachineFingerprint();
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    static String getMachineFingerprint() {
        StringBuilder sb = new StringBuilder();

        // 1. Motherboard serial (Windows only)
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"wmic", "baseboard", "get", "serialnumber"});
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            // Output: "SerialNumber\n<value>"
            String[] lines = out.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("SerialNumber")) {
                    sb.append("MB:").append(trimmed);
                    break;
                }
            }
        } catch (Exception ignored) {}

        // 2. CPU ID (Windows only)
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"wmic", "cpu", "get", "ProcessorId"});
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String[] lines = out.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("ProcessorId")) {
                    sb.append("CPU:").append(trimmed);
                    break;
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback: hostname + OS
        if (sb.isEmpty()) {
            try { sb.append("HOST:").append(java.net.InetAddress.getLocalHost().getHostName()); }
            catch (Exception ignored) {}
            sb.append("OS:").append(System.getProperty("os.name", "unknown"));
        }

        return sb.toString();
    }

    private static SecretKey toSecretKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LEN];
        new java.security.SecureRandom().nextBytes(iv);
        return iv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLI helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Two modes:
     *
     *   --setup-db <db.properties path>
     *       Reads the file, encrypts the plain-text db.password in place,
     *       and writes db.password.enc back.  Used by the InnoSetup installer
     *       (runs as admin, so write access is guaranteed).
     *
     *   <plaintext-password>
     *       Prints the encrypted value for manual copy-paste.
     */
    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && "--setup-db".equals(args[0])) {
            setupDbProperties(java.nio.file.Paths.get(args[1]));
            return;
        }
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("  CredentialCrypto --setup-db <db.properties path>");
            System.out.println("  CredentialCrypto <plaintext-password>");
            System.out.println("Machine fingerprint: " + getMachineFingerprint());
            return;
        }
        String encrypted = encrypt(args[0]);
        System.out.println("Machine fingerprint : " + getMachineFingerprint());
        System.out.println("Encrypted password  : " + encrypted);
        System.out.println();
        System.out.println("Add this line to db.properties:");
        System.out.println("db.password.enc=" + encrypted);
    }

    /**
     * Reads db.properties at {@code propsPath}, finds the plain-text
     * {@code db.password=<value>} line, encrypts the value with the
     * machine fingerprint key, replaces the line with
     * {@code db.password.enc=ENC:…}, and writes the file back.
     *
     * No-ops if the file already contains {@code db.password.enc}.
     */
    public static void setupDbProperties(java.nio.file.Path propsPath) throws Exception {
        if (!propsPath.toFile().exists()) {
            System.err.println("setupDbProperties: file not found: " + propsPath);
            System.exit(1);
        }

        java.util.List<String> lines = java.nio.file.Files.readAllLines(
                propsPath, java.nio.charset.StandardCharsets.UTF_8);
        java.util.List<String> updated = new java.util.ArrayList<>();
        boolean upgraded = false;

        for (String line : lines) {
            if (line.startsWith("db.password=") && !line.startsWith("db.password.enc=")) {
                String plain = line.substring("db.password=".length());
                updated.add("db.password.enc=" + encrypt(plain));
                upgraded = true;
                System.out.println("db.password encrypted -> db.password.enc");
            } else {
                updated.add(line);
            }
        }

        if (upgraded) {
            java.nio.file.Files.write(propsPath, updated, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Saved: " + propsPath);
        } else {
            System.out.println("Nothing to do (no plain db.password found).");
        }
    }
}