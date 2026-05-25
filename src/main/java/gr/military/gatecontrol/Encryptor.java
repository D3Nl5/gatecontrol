package gr.military.gatecontrol;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

/**
 * Standalone password encryptor — no dependencies, no Spring Boot needed.
 *
 * Compile:  javac Encryptor.java
 * Run:      java Encryptor
 *
 * Paste the output line into db.properties as:
 *   db.password.enc=ENC:...
 * Then comment out:
 *   #db.password=
 */
public class Encryptor {

    private static final String PREFIX     = "ENC:";
    private static final int    GCM_IV_LEN = 12;
    private static final int    GCM_TAG    = 128;

    public static void main(String[] args) throws Exception {
        String password;

        if (args.length > 0) {
            password = args[0];
        } else {
            System.out.print("Enter the database password to encrypt: ");
            System.out.flush();
            if (System.console() != null) {
                char[] chars = System.console().readPassword();
                password = new String(chars);
            } else {
                Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
                password = sc.nextLine();
            }
        }

        String fingerprint = getMachineFingerprint();
        String encrypted   = encrypt(password, fingerprint);

        System.out.println();
        System.out.println("Machine fingerprint : " + fingerprint);
        System.out.println();
        System.out.println("Add this line to db.properties:");
        System.out.println("db.password.enc=" + encrypted);
        System.out.println();
        System.out.println("Then comment out:  #db.password=");
    }

    static String encrypt(String plaintext, String fingerprint) throws Exception {
        byte[] key = deriveKey(fingerprint);
        byte[] iv  = new byte[GCM_IV_LEN];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG, iv));
        byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

        return PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    static byte[] deriveKey(String fingerprint) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    static String getMachineFingerprint() {
        StringBuilder sb = new StringBuilder();

        // Motherboard serial
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"wmic", "baseboard", "get", "serialnumber"});
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            for (String line : out.split("\\r?\\n")) {
                String t = line.trim();
                if (!t.isEmpty() && !t.equalsIgnoreCase("SerialNumber")) {
                    sb.append("MB:").append(t);
                    break;
                }
            }
        } catch (Exception ignored) {}

        // CPU ID
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"wmic", "cpu", "get", "ProcessorId"});
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            for (String line : out.split("\\r?\\n")) {
                String t = line.trim();
                if (!t.isEmpty() && !t.equalsIgnoreCase("ProcessorId")) {
                    sb.append("CPU:").append(t);
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Fallback
        if (sb.isEmpty()) {
            try { sb.append("HOST:").append(java.net.InetAddress.getLocalHost().getHostName()); }
            catch (Exception ignored) {}
            sb.append("OS:").append(System.getProperty("os.name", "unknown"));
        }

        return sb.toString();
    }
}