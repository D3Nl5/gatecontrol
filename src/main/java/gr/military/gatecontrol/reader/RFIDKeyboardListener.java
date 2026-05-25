package gr.military.gatecontrol.reader;

import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Stdin-based RFID reader.
 * Currently unused – card UIDs are captured via the JavaFX scene keyboard listener instead.
 * Kept for potential headless / testing use.
 */
@Component
public class RFIDKeyboardListener implements Runnable {

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("RFID Keyboard Listener Started...");

        while (true) {
            String uid = scanner.nextLine();
            if (uid != null && !uid.isBlank()) {
                // TODO: wire to GateAccessService if stdin reader is needed
            }
        }
    }
}