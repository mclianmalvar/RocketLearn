package rocketlearn;

/**
 * When a wrong answer is explained: on every miss, only on the miss that
 * lights up the right asteroid (and at game over), or never. Practice
 * always explains, whatever this says - explaining is what practice is for.
 */
enum ExplainMode {
    EVERY("Every Miss", "Explain why after every wrong answer"),
    HINT("Hint Round", "Explain why once the right answer lights up, and at game over"),
    OFF("Off", "Never explain during a run - practice still always explains");

    final String label;
    /** The Settings hint for this choice. */
    final String hint;

    ExplainMode(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    /** The next choice along, wrapping round at either end. */
    ExplainMode step(int dir) {
        ExplainMode[] all = values();
        return all[((ordinal() + dir) % all.length + all.length) % all.length];
    }

    /**
     * Reads a saved choice back. Older saves wrote it as a number (0, 1, 2),
     * so those still load; anything unrecognised falls back to the hint round.
     */
    static ExplainMode parse(String saved) {
        String s = saved == null ? "" : saved.trim();
        for (ExplainMode m : values()) {
            if (m.name().equalsIgnoreCase(s) || String.valueOf(m.ordinal()).equals(s)) return m;
        }
        return HINT;
    }
}
