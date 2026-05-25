package gr.military.gatecontrol.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Admin authentication backed by PBKDF2WithHmacSHA256.
 * Legacy SHA-256 hashes (stored before this upgrade) are accepted and
 * silently upgraded to PBKDF2 on the next successful login.
 *
 * db.properties entry:
 *   admin.password.hash=PBKDF2:<iterations>:<salt_hex>:<hash_hex>
 *
 * Default password is "1234" (stored as legacy SHA-256) — change it after first login.
 */
public class AdminAuth {

    private static final String PBKDF2_PREFIX = "PBKDF2:";
    private static final int    ITERATIONS    = 260_000;
    private static final int    KEY_BITS      = 256;

    private static String storedHash = null;

    // SHA-256 of "1234" — legacy default, auto-upgraded to PBKDF2 on first login
    private static final String DEFAULT_HASH =
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4";

    public static void setHashFromProperties(String hash) {
        if (hash != null && !hash.isBlank()) storedHash = hash.trim();
    }

    /** True when the stored hash is the old SHA-256 format and should be upgraded. */
    public static boolean needsHashUpgrade() {
        String hash = storedHash != null ? storedHash : DEFAULT_HASH;
        return !hash.startsWith(PBKDF2_PREFIX);
    }

    public static boolean verify(String enteredPassword) {
        if (enteredPassword == null || enteredPassword.isEmpty()) return false;
        String hash = storedHash != null ? storedHash : DEFAULT_HASH;
        if (hash.startsWith(PBKDF2_PREFIX)) return verifyPbkdf2(enteredPassword, hash);
        return hash.equalsIgnoreCase(sha256hex(enteredPassword));
    }

    /** Produces a new PBKDF2 hash string suitable for storage in db.properties. */
    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_BITS);
            return PBKDF2_PREFIX + ITERATIONS + ":" + hex(salt) + ":" + hex(hash);
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 hashing failed", e);
        }
    }

    private static boolean verifyPbkdf2(String password, String stored) {
        try {
            String[] parts    = stored.substring(PBKDF2_PREFIX.length()).split(":");
            int      iters    = Integer.parseInt(parts[0]);
            byte[]   salt     = unhex(parts[1]);
            byte[]   expected = unhex(parts[2]);
            byte[]   actual   = pbkdf2(password, salt, iters, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyBits)
            throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyBits);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    private static String sha256hex(String password) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            return hex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] unhex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return out;
    }
}
