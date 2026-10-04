package rocketlearn;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Lifetime totals that survive across runs, so the game can tell the player
 * which subject they are actually weak at rather than only which one cost them
 * the last game.
 *
 * Per-topic figures are stored as "topicstat.<topic>=correct/attempts".
 */
class Stats {

    int answered, correct;
    final Map<String, int[]> byTopic = new LinkedHashMap<>();

    void record(String topic, boolean wasCorrect) {
        answered++;
        if (wasCorrect) correct++;
        int[] t = byTopic.computeIfAbsent(topic, k -> new int[2]);
        t[1]++;
        if (wasCorrect) t[0]++;
    }

    int accuracyPercent() {
        return answered == 0 ? 0 : (int) Math.round(correct * 100.0 / answered);
    }

    int topicAccuracy(String topic) {
        int[] t = byTopic.get(topic);
        if (t == null || t[1] == 0) return 0;
        return (int) Math.round(t[0] * 100.0 / t[1]);
    }

    /**
     * The topic with the lowest accuracy, ignoring any answered fewer than
     * MIN_SAMPLES times so one unlucky question cannot dominate the verdict.
     */
    static final int MIN_SAMPLES = 3;

    String weakestTopic() {
        String worst = null;
        int worstPct = 101;
        for (Map.Entry<String, int[]> e : byTopic.entrySet()) {
            int[] t = e.getValue();
            if (t[1] < MIN_SAMPLES) continue;
            int pct = (int) Math.round(t[0] * 100.0 / t[1]);
            if (pct < worstPct) { worstPct = pct; worst = e.getKey(); }
        }
        return worst;
    }

    boolean isEmpty() { return answered == 0; }

    void load(Properties p) {
        answered = readInt(p, "statAnswered");
        correct = Math.min(answered, readInt(p, "statCorrect"));
        byTopic.clear();
        for (String key : p.stringPropertyNames()) {
            if (!key.startsWith("topicstat.")) continue;
            String topic = key.substring("topicstat.".length());
            String[] parts = p.getProperty(key, "").split("/");
            if (parts.length != 2) continue;
            try {
                int c = Integer.parseInt(parts[0].trim());
                int a = Integer.parseInt(parts[1].trim());
                if (a > 0 && c >= 0 && c <= a) byTopic.put(topic, new int[]{c, a});
            } catch (NumberFormatException ignored) { }
        }
    }

    void save(Properties p) {
        p.setProperty("statAnswered", String.valueOf(answered));
        p.setProperty("statCorrect", String.valueOf(correct));
        for (Map.Entry<String, int[]> e : byTopic.entrySet()) {
            p.setProperty("topicstat." + e.getKey(), e.getValue()[0] + "/" + e.getValue()[1]);
        }
    }

    private static int readInt(Properties p, String key) {
        try { return Math.max(0, Integer.parseInt(p.getProperty(key, "0").trim())); }
        catch (NumberFormatException e) { return 0; }
    }
}
