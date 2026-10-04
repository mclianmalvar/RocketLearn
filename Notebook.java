package rocketlearn;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Every question the player has got wrong, kept across runs so its
 * explanation can be reread from the menu and the question drilled until it
 * sticks.
 *
 * A question is "cleared" once it is later answered right on the first try,
 * and missing it again puts it back on the list to clear. Cleared questions
 * stay in the notebook to reread; they only drop out of the drill.
 *
 * In between, a question answered right on a retry after missing it is
 * "recovered": partly learned, and shown as such, but still to clear.
 *
 * Clearing is not the end: a cleared question comes back for review a day
 * later, then three days after that, then a week, because a thing recalled
 * just as it starts to fade is what makes it stick. Each review answered
 * right first try pushes the next one further out; passing the last retires
 * it. Missing a review puts the question back on the list to clear.
 *
 * Saved as "notebook=key,key,..." newest first, plus "notebookCleared=...",
 * "notebookRecovered=..." and "notebookReview=key:step:day,...".
 * A key is a hash of the question's text rather than its position in the
 * bank, so adding or reordering questions does not scramble a saved notebook.
 */
class Notebook {

    static final int MAX_ENTRIES = 80;

    /** Newest miss first. */
    final List<Question> entries = new ArrayList<>();
    /** The same questions as entries, for quick "is it in the notebook" checks. Kept in step with it. */
    private final Set<Question> inBook = new HashSet<>();
    final Set<Question> cleared = new HashSet<>();
    /** Uncleared questions the player has since got right on a retry. */
    final Set<Question> recovered = new HashSet<>();

    /** Days from one review to the next: after clearing, then after each review passed. */
    static final int[] REVIEW_DAYS = { 1, 3, 7 };

    /**
     * Cleared questions still being reviewed: how many reviews each has
     * passed, and the day (counted as LocalDate epoch days) the next is due.
     * A cleared question missing from here has passed them all.
     */
    final Map<Question, Integer> reviewStep = new HashMap<>();
    final Map<Question, Long> reviewDue = new HashMap<>();

    /** Today, as an epoch day. A field so the tests can move the calendar. */
    LongSupplier today = () -> LocalDate.now().toEpochDay();

    /** Every question in the bank by key, and the tier each belongs to. */
    static final Map<String, Question> BY_KEY = new HashMap<>();
    static final Map<Question, Integer> TIER_OF = new HashMap<>();
    static {
        for (int t = 0; t < QuestionBank.TIERS.size(); t++) {
            for (Question q : QuestionBank.TIERS.get(t)) {
                BY_KEY.put(keyOf(q), q);
                TIER_OF.put(q, t);
            }
        }
    }

    static String keyOf(Question q) {
        String text = q.prompt + "\u0000" + (q.code == null ? "" : q.code) + "\u0000" + q.correctAnswer();
        return Integer.toHexString(text.hashCode());
    }

    static int tierOf(Question q) {
        Integer t = TIER_OF.get(q);
        return t == null ? 0 : t;
    }

    boolean isEmpty() { return entries.isEmpty(); }

    int size() { return entries.size(); }

    boolean has(Question q) { return inBook.contains(q); }

    boolean isCleared(Question q) { return cleared.contains(q); }

    boolean isRecovered(Question q) { return recovered.contains(q); }

    /** The questions still to clear, newest miss first. */
    List<Question> toClear() {
        List<Question> out = new ArrayList<>();
        for (Question q : entries) if (!cleared.contains(q)) out.add(q);
        return out;
    }

    /** Cleared questions whose review has come round, newest miss first. */
    List<Question> dueForReview() {
        List<Question> out = new ArrayList<>();
        for (Question q : entries) if (isDueForReview(q)) out.add(q);
        return out;
    }

    boolean isDueForReview(Question q) {
        Long due = reviewDue.get(q);
        return due != null && cleared.contains(q) && due <= today.getAsLong();
    }

    /** What the mistakes drill asks: everything still to clear, then every review that is due. */
    List<Question> toPractice() {
        List<Question> out = toClear();
        out.addAll(dueForReview());
        return out;
    }

    /** How many are still to clear - toClear().size() without building the list. */
    int toClearCount() { return entries.size() - cleared.size(); }

    /** How many reviews are due - dueForReview().size() without building the list. */
    int dueCount() {
        int n = 0;
        for (Question q : reviewDue.keySet()) if (isDueForReview(q)) n++;
        return n;
    }

    /** How many the mistakes drill has left to ask. */
    int practiceCount() { return toClearCount() + dueCount(); }

    /** Days until a cleared question's next review, 0 when due now, or -1 when it has none left. */
    long daysUntilReview(Question q) {
        Long due = reviewDue.get(q);
        return due == null ? -1 : Math.max(0, due - today.getAsLong());
    }

    /** A wrong answer: the question moves to the top and has to be cleared again. */
    void recordMiss(Question q) {
        if (q == null) return;
        entries.remove(q);
        entries.add(0, q);
        inBook.add(q);
        cleared.remove(q);
        recovered.remove(q);
        forgetReview(q);
        trim();
    }

    private void forgetReview(Question q) {
        reviewStep.remove(q);
        reviewDue.remove(q);
    }

    /** A right answer on a retry: partly learned, though it still has to be cleared first try. */
    void recordRecovery(Question q) {
        if (q != null && has(q) && !cleared.contains(q)) recovered.add(q);
    }

    /**
     * A right answer on the first try. Returns true when that clears a
     * question that was waiting to be cleared. Clearing books its first
     * review; passing a review that was due books the next one, or retires
     * the question after the last.
     */
    boolean recordClear(Question q) {
        if (q == null || !has(q)) return false;
        recovered.remove(q);
        long now = today.getAsLong();
        if (cleared.add(q)) {
            reviewStep.put(q, 0);
            reviewDue.put(q, now + REVIEW_DAYS[0]);
            return true;
        }
        if (isDueForReview(q)) {
            int step = reviewStep.getOrDefault(q, 0) + 1;
            if (step >= REVIEW_DAYS.length) {
                forgetReview(q);
            } else {
                reviewStep.put(q, step);
                reviewDue.put(q, now + REVIEW_DAYS[step]);
            }
        }
        return false;
    }

    /** Drops the oldest cleared questions first, and only then the oldest of the rest. */
    private void trim() {
        for (int i = entries.size() - 1; i >= 0 && entries.size() > MAX_ENTRIES; i--) {
            if (cleared.contains(entries.get(i))) {
                Question q = entries.remove(i);
                inBook.remove(q);
                cleared.remove(q);
                forgetReview(q);
            }
        }
        while (entries.size() > MAX_ENTRIES) {
            Question q = entries.remove(entries.size() - 1);
            inBook.remove(q);
            recovered.remove(q);
        }
    }

    void load(Properties p) {
        entries.clear();
        inBook.clear();
        cleared.clear();
        recovered.clear();
        reviewStep.clear();
        reviewDue.clear();
        for (String key : p.getProperty("notebook", "").split(",")) {
            Question q = BY_KEY.get(key.trim());
            if (q != null && inBook.add(q)) entries.add(q);
        }
        for (String key : p.getProperty("notebookCleared", "").split(",")) {
            Question q = BY_KEY.get(key.trim());
            if (q != null && has(q)) cleared.add(q);
        }
        for (String key : p.getProperty("notebookRecovered", "").split(",")) {
            Question q = BY_KEY.get(key.trim());
            if (q != null && has(q) && !cleared.contains(q)) recovered.add(q);
        }
        // A save from before reviews existed has none booked: its cleared questions count as learned.
        for (String item : p.getProperty("notebookReview", "").split(",")) {
            String[] parts = item.trim().split(":");
            if (parts.length != 3) continue;
            Question q = BY_KEY.get(parts[0]);
            if (q == null || !cleared.contains(q)) continue;
            try {
                int step = Integer.parseInt(parts[1]);
                long due = Long.parseLong(parts[2]);
                if (step < 0 || step >= REVIEW_DAYS.length) continue;
                reviewStep.put(q, step);
                reviewDue.put(q, due);
            } catch (NumberFormatException ignored) { }
        }
        trim();
    }

    void save(Properties p) {
        List<String> all = new ArrayList<>(), done = new ArrayList<>(), partly = new ArrayList<>(), review = new ArrayList<>();
        for (Question q : entries) {
            all.add(keyOf(q));
            if (cleared.contains(q)) done.add(keyOf(q));
            if (recovered.contains(q)) partly.add(keyOf(q));
            if (reviewDue.containsKey(q)) review.add(keyOf(q) + ":" + reviewStep.getOrDefault(q, 0) + ":" + reviewDue.get(q));
        }
        p.setProperty("notebook", String.join(",", all));
        p.setProperty("notebookCleared", String.join(",", done));
        p.setProperty("notebookRecovered", String.join(",", partly));
        p.setProperty("notebookReview", String.join(",", review));
    }
}
