package rocketlearn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * The top five runs, saved alongside the rest of the player's progress.
 * Each entry is stored as one line of "NAME:SCORE:LEVEL".
 */
class HighScores {

    static final int MAX_ENTRIES = 5;
    static final int NAME_LENGTH = 3;

    static class Entry implements Comparable<Entry> {
        final String name;
        final int score, level;

        Entry(String name, int score, int level) {
            this.name = name;
            this.score = score;
            this.level = level;
        }

        @Override
        public int compareTo(Entry other) {
            return Integer.compare(other.score, this.score);
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    List<Entry> all() { return entries; }

    boolean isEmpty() { return entries.isEmpty(); }

    /** A score qualifies while the board has room, or once it beats the last place. */
    boolean qualifies(int score) {
        if (score <= 0) return false;
        if (entries.size() < MAX_ENTRIES) return true;
        return score > entries.get(entries.size() - 1).score;
    }

    /** Adds an entry and returns its position, or -1 when it did not make the board. */
    int add(String name, int score, int level) {
        if (!qualifies(score)) return -1;
        Entry e = new Entry(sanitize(name), score, level);
        entries.add(e);
        Collections.sort(entries);
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        return entries.indexOf(e);
    }

    static String sanitize(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length() && sb.length() < NAME_LENGTH; i++) {
            char c = Character.toUpperCase(raw.charAt(i));
            sb.append(c >= 'A' && c <= 'Z' ? c : '-');
        }
        while (sb.length() < NAME_LENGTH) sb.append('-');
        return sb.toString();
    }

    void load(Properties p) {
        entries.clear();
        for (int i = 0; i < MAX_ENTRIES; i++) {
            String raw = p.getProperty("hs" + i);
            if (raw == null) continue;
            String[] parts = raw.split(":");
            if (parts.length != 3) continue;
            try {
                entries.add(new Entry(sanitize(parts[0]), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
            } catch (NumberFormatException ignored) { }
        }
        Collections.sort(entries);
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
    }

    void save(Properties p) {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            if (i < entries.size()) {
                Entry e = entries.get(i);
                p.setProperty("hs" + i, e.name + ":" + e.score + ":" + e.level);
            } else {
                p.remove("hs" + i);
            }
        }
    }
}
