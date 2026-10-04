package rocketlearn;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reads text aloud with the computer's own voice, for players who find
 * reading the questions hard. Java has no speech of its own, so this asks
 * the system: Windows' built-in voice through PowerShell, "say" on a Mac,
 * espeak on Linux. The text is handed over on standard input, never put on
 * a command line, so nothing in a question can be run as a command.
 *
 * Only one thing is spoken at a time: new speech cuts off the old.
 */
final class Speech {

    /**
     * Where speech goes. The tests swap in a recorder, so they never start
     * a process; null means use the system voice.
     */
    static Consumer<String> engine;

    private static Process speaking;

    /** Speaks the text, cutting off anything still being said. Returns at once. */
    static synchronized void say(String text) {
        if (text == null || text.isBlank()) return;
        if (engine != null) { engine.accept(text); return; }
        stop();
        String[] cmd = command();
        if (cmd == null) return;
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            try (OutputStream in = p.getOutputStream()) {
                in.write(clean(text).getBytes(StandardCharsets.UTF_8));
            }
            speaking = p;
        } catch (IOException | SecurityException e) {
            // No voice on this computer: the game carries on silently.
        }
    }

    static synchronized void stop() {
        if (engine != null) return;
        if (speaking != null && speaking.isAlive()) speaking.destroy();
        speaking = null;
    }

    /** The command that reads standard input aloud on this system, or null when there is none. */
    static String[] command() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new String[]{ "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "Add-Type -AssemblyName System.Speech; $v = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$v.Speak([Console]::In.ReadToEnd())" };
        }
        if (os.contains("mac")) return new String[]{ "say" };
        return new String[]{ "espeak", "--stdin" };
    }

    /** Text made easier to say: the code blank becomes "blank", and runs of spaces shrink. */
    static String clean(String text) {
        return text.replace(Question.BLANK, " blank ").replaceAll("\\s+", " ").trim();
    }

    private Speech() { }
}
