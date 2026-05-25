package gr.military.gatecontrol.reader;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reads card IDs from a PC/SC NFC reader (e.g. ACR122U).
 *
 * Works alongside the existing RFID keyboard listener — both call the same
 * handleCardRead() callback in GateUIWithRoster.
 *
 * Protocol (matches GateCardService.java on Android):
 *   1. SELECT AID  F047617465436172  →  90 00
 *   2. GET DATA    00 CA 00 00 00    →  [UTF-8 card ID] 90 00
 */
public class NfcReaderService {

    private static final Logger log = LoggerFactory.getLogger(NfcReaderService.class);

    // AID: F047617465436172 = "F0GateCar" (proprietary, matches Android app)
    private static final byte[] AID = {
            (byte) 0xF0, 0x47, 0x61, 0x74, 0x65, 0x43, 0x61, 0x72
    };

    // SELECT AID APDU
    private static final byte[] SELECT_AID_APDU = buildSelectApdu(AID);

    // GET DATA APDU
    private static final byte[] GET_DATA_APDU = {0x00, (byte) 0xCA, 0x00, 0x00, 0x00};

    private volatile boolean running = false;
    private Thread readerThread;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Start polling for NFC phones/cards in a background daemon thread.
     *
     * @param onCardRead called on the JavaFX thread when a valid card ID is read
     */
    public void start(Consumer<String> onCardRead) {
        if (running) return;
        running = true;

        readerThread = new Thread(() -> {
            log.info("NFC reader service started");
            while (running) {
                try {
                    CardTerminal terminal = findTerminal();
                    if (terminal == null) {
                        Thread.sleep(3000);   // no reader connected — check again in 3s
                        continue;
                    }
                    log.info("NFC reader found: {}", terminal.getName());
                    pollTerminal(terminal, onCardRead);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("NFC reader error: {}", e.getMessage());
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); break;
                    }
                }
            }
            log.info("NFC reader service stopped");
        }, "NFC-Reader");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void stop() {
        running = false;
        if (readerThread != null) readerThread.interrupt();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Find the first available PC/SC terminal, or null if none connected. */
    private CardTerminal findTerminal() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            return terminals.isEmpty() ? null : terminals.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Block until a phone/card is presented, read its ID, fire the callback,
     * then wait for removal before returning (so the same tap is only processed once).
     */
    private void pollTerminal(CardTerminal terminal, Consumer<String> onCardRead)
            throws CardException, InterruptedException {

        while (running) {
            // Wait for card present (timeout 1s so we can check 'running' regularly)
            boolean present = terminal.waitForCardPresent(1000);
            if (!present) continue;

            try {
                Card card = terminal.connect("*");
                try {
                    String cardId = readCardId(card.getBasicChannel());
                    if (cardId != null) {
                        log.info("NFC card read: {}", cardId);
                        Platform.runLater(() -> onCardRead.accept(cardId));
                    } else {
                        log.debug("NFC: card present but not a GateCard (no matching AID)");
                    }
                } finally {
                    card.disconnect(false);
                }
            } catch (CardException e) {
                log.debug("NFC connect error (phone removed quickly?): {}", e.getMessage());
            }

            // Wait for card to be removed before accepting next tap
            terminal.waitForCardAbsent(5000);
        }
    }

    /**
     * Send SELECT AID + GET DATA to the card/phone.
     * Returns the card ID string, or null if the AID is not recognised.
     */
    private String readCardId(CardChannel channel) throws CardException {
        // 1. SELECT AID
        ResponseAPDU selectResp = channel.transmit(new CommandAPDU(SELECT_AID_APDU));
        if (selectResp.getSW() != 0x9000) {
            return null;  // not our app
        }

        // 2. GET DATA
        ResponseAPDU dataResp = channel.transmit(new CommandAPDU(GET_DATA_APDU));
        if (dataResp.getSW() != 0x9000 || dataResp.getData().length == 0) {
            return null;
        }

        return new String(dataResp.getData(), StandardCharsets.UTF_8).trim();
    }

    // ── APDU builder ──────────────────────────────────────────────────────────

    private static byte[] buildSelectApdu(byte[] aid) {
        // 00 A4 04 00 <Lc = len(AID)> <AID> 00
        byte[] apdu = new byte[6 + aid.length];
        apdu[0] = 0x00;           // CLA
        apdu[1] = (byte) 0xA4;   // INS: SELECT FILE
        apdu[2] = 0x04;           // P1:  select by AID
        apdu[3] = 0x00;           // P2
        apdu[4] = (byte) aid.length; // Lc
        System.arraycopy(aid, 0, apdu, 5, aid.length);
        apdu[5 + aid.length] = 0x00; // Le
        return apdu;
    }
}
