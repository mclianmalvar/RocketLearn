package rocketlearn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which question comes next. Each difficulty tier is dealt like a
 * shuffled deck, so nothing repeats until the tier has been gone through;
 * on top of that sit the mistakes drill (only notebook questions), a
 * teacher's topic focus for a planet, and practice leaning toward a topic.
 */
class QuestionDealer {

    final RocketLearn game;

    QuestionDealer(RocketLearn game) { this.game = game; }

    /** The questions in play, by tier: a teacher's questions.txt or the built-in set. */
    List<List<Question>> bank = QuestionBank.TIERS;
    /** What is left to deal from each tier before it is dealt again. */
    List<List<Question>> pools = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    /** Pools of a focus topic's questions, dealt like the tier pools, keyed by tier and topic. */
    final Map<String, List<Question>> focusPools = new HashMap<>();

    /** Which bank the current question came from - it is not always the sector's. */
    int currentTier = 0;

    /** Starts every deck afresh, as a new run does. */
    void reset() {
        for (List<Question> p : pools) p.clear();
        focusPools.clear();
    }

    /** Deals the next question into the game's currentQuestion. */
    void pickQuestion() {
        // Flight School asks its one easy question.
        if (game.flightSchoolRun) {
            take(FlightSchool.QUESTION, 0);
            return;
        }
        // The daily flight asks its own ten, in order.
        if (game.dailyRun && game.daily.current() != null) {
            Question q = game.daily.current();
            Integer t = tierIndex().get(q);
            take(q, t == null ? 0 : t);
            return;
        }
        // The mistakes drill deals only from the notebook, never the same
        // question twice running while there is another to ask.
        if (game.mistakesDrill) {
            List<Question> left = game.notebook.toPractice();
            if (!left.isEmpty()) {
                if (left.size() > 1) left.remove(game.currentQuestion);
                Question q = left.get(game.rng.nextInt(left.size()));
                take(q, Notebook.tierOf(q));
                return;
            }
        }

        // The star is a summary: it draws from every bank, in no order at all.
        int tierIdx = game.atStar() ? game.rng.nextInt(bank.size()) : game.difficultySector();
        currentTier = tierIdx;

        // A planet the teacher has given a topic asks only that topic.
        String focus = planetFocus();
        if (focus != null) {
            Question q = drawFocused(focus, tierIdx);
            if (q != null) {
                take(q, currentTier);
                return;
            }
        }

        List<Question> pool = pools.get(tierIdx);
        if (pool.isEmpty()) refillPool(tierIdx);

        // In practice, favour the focus topic. Not every tier has every topic -
        // the easy bank has no Algorithms at all - so when this tier has none
        // left, reach into the others rather than never asking it.
        if (game.practiceMode && game.practiceFocus != null && game.rng.nextDouble() < 0.6) {
            for (int k = 0; k < pools.size(); k++) {
                int t = (tierIdx + k) % pools.size();
                List<Question> p = pools.get(t);
                if (p.isEmpty()) refillPool(t);
                for (int i = p.size() - 1; i >= 0; i--) {
                    if (game.practiceFocus.equals(p.get(i).topic)) {
                        take(p.remove(i), t);
                        return;
                    }
                }
            }
        }
        take(pool.remove(pool.size() - 1), tierIdx);
    }

    private void take(Question q, int tier) {
        currentTier = tier;
        game.currentQuestion = q;
        game.questionText = q.prompt;
    }

    /** The teacher's topic for the planet being flown, or null. Practice and the star keep their own mix. */
    String planetFocus() {
        if (game.practiceMode || game.atStar()) return null;
        return QuestionFile.planetFocus.get(game.planetIndex());
    }

    /**
     * The next question on a focus topic: from this difficulty's questions on
     * it, or from every difficulty when this one has none. Null only when no
     * question in the bank has the topic at all. Sets currentTier to the tier
     * the question came from.
     */
    Question drawFocused(String topic, int tier) {
        List<Question> pool = focusPools.computeIfAbsent(tier + ":" + topic, k -> new ArrayList<>());
        if (pool.isEmpty()) {
            for (Question q : bank.get(tier)) if (topic.equals(q.topic)) pool.add(q);
            if (pool.isEmpty()) for (List<Question> t : bank) for (Question q : t) if (topic.equals(q.topic)) pool.add(q);
            if (QuestionFile.fixedOrder) Collections.reverse(pool);
            else Collections.shuffle(pool, game.rng);
        }
        if (pool.isEmpty()) return null;
        Question q = pool.remove(pool.size() - 1);
        Integer t = tierIndex().get(q);
        if (t != null) currentTier = t;
        return q;
    }

    /** Which tier each question in the bank is in, built once per bank rather than searched for. */
    private Map<Question, Integer> tierIndex;
    private List<List<Question>> indexedBank;

    private Map<Question, Integer> tierIndex() {
        if (indexedBank != bank) {
            tierIndex = new java.util.IdentityHashMap<>();
            for (int t = 0; t < bank.size(); t++) for (Question q : bank.get(t)) tierIndex.put(q, t);
            indexedBank = bank;
        }
        return tierIndex;
    }

    /**
     * Deals a tier's questions back into its pool, which is drawn from the
     * end. Shuffled, unless a teacher's file asked for its order kept: then
     * reversed, so the questions come out top to bottom as written.
     */
    void refillPool(int tier) {
        List<Question> pool = pools.get(tier);
        pool.addAll(bank.get(tier));
        if (QuestionFile.fixedOrder) Collections.reverse(pool);
        else Collections.shuffle(pool, game.rng);
    }
}
