package rocketlearn;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Ten questions a day, the same ten for everyone with the same questions on
 * that date, so a class can compare scores and the habit of coming back
 * each day is what the spaced review in the notebook needs.
 *
 * The questions are picked from the date alone: three easy, four medium and
 * three hard, shuffled together. The first finish of a day counts towards
 * the streak; flying it again the same day is practice.
 *
 * Saved as dailyLastDay, dailyLastScore, dailyStreak and dailyBestStreak.
 */
class DailyFlight {

    static final int QUESTIONS = 10;
    static final int[] PER_TIER = { 3, 4, 3 };

    /** Today, as an epoch day. A field so the tests can move the calendar. */
    LongSupplier today = () -> LocalDate.now().toEpochDay();

    /** The last day a daily flight was finished (epoch day), or Long.MIN_VALUE for never. */
    long lastDay = Long.MIN_VALUE;
    int lastScore, streak, bestStreak;

    // ---- the flight in progress
    List<Question> questions = List.of();
    int asked, rightFirstTry;

    boolean doneToday() { return lastDay == today.getAsLong(); }

    /** The streak as it stands today: one missed day breaks it. */
    int currentStreak() {
        long t = today.getAsLong();
        return lastDay == t || lastDay == t - 1 ? streak : 0;
    }

    /** The ten questions for a day, from the given bank. Never fewer than the bank allows. */
    static List<Question> questionsFor(long day, List<List<Question>> bank) {
        Random r = new Random(day * 1_000_003L + 17);
        List<Question> out = new ArrayList<>();
        for (int t = 0; t < bank.size(); t++) {
            List<Question> tier = new ArrayList<>(bank.get(t));
            Collections.shuffle(tier, r);
            int want = t < PER_TIER.length ? PER_TIER[t] : 0;
            out.addAll(tier.subList(0, Math.min(want, tier.size())));
        }
        // A small bank: top up from anything not already picked.
        if (out.size() < QUESTIONS) {
            List<Question> rest = new ArrayList<>();
            for (List<Question> t : bank) for (Question q : t) if (!out.contains(q)) rest.add(q);
            Collections.shuffle(rest, r);
            out.addAll(rest.subList(0, Math.min(QUESTIONS - out.size(), rest.size())));
        }
        Collections.shuffle(out, r);
        return out;
    }

    void start(List<List<Question>> bank) {
        questions = questionsFor(today.getAsLong(), bank);
        asked = 0;
        rightFirstTry = 0;
    }

    Question current() { return asked < questions.size() ? questions.get(asked) : null; }

    boolean finished() { return asked >= questions.size(); }

    /** A question answered right and moved past. */
    void answered(boolean firstTry) {
        asked++;
        if (firstTry) rightFirstTry++;
    }

    /** Records a finished flight. Returns true when it was today's first, which is the one that counts. */
    boolean record() {
        long t = today.getAsLong();
        if (lastDay == t) return false;
        streak = lastDay == t - 1 ? streak + 1 : 1;
        bestStreak = Math.max(bestStreak, streak);
        lastDay = t;
        lastScore = rightFirstTry;
        return true;
    }

    void load(Properties p) {
        try { lastDay = Long.parseLong(p.getProperty("dailyLastDay", String.valueOf(Long.MIN_VALUE)).trim()); }
        catch (NumberFormatException e) { lastDay = Long.MIN_VALUE; }
        lastScore = clamp(SaveFile.readInt(p, "dailyLastScore", 0), 0, QUESTIONS);
        streak = Math.max(0, SaveFile.readInt(p, "dailyStreak", 0));
        bestStreak = Math.max(streak, SaveFile.readInt(p, "dailyBestStreak", 0));
    }

    void save(Properties p) {
        if (lastDay == Long.MIN_VALUE) return;
        p.setProperty("dailyLastDay", String.valueOf(lastDay));
        p.setProperty("dailyLastScore", String.valueOf(lastScore));
        p.setProperty("dailyStreak", String.valueOf(streak));
        p.setProperty("dailyBestStreak", String.valueOf(bestStreak));
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
