package rocketlearn;

import java.awt.Color;
import java.util.Arrays;
import java.util.Properties;

/**
 * What the player has achieved at each planet: whether they have finished
 * it, and the best medal they have earned flying it.
 *
 * A planet is finished by answering its last question in a real run (not
 * practice). How well it went decides the medal:
 *
 *   Gold   - no hearts lost there, and 90% or more answered right first try
 *   Silver - at most one heart lost, and 75% or more right first try
 *   Bronze - finished at all
 *
 * Silver and gold also need the planet flown from its first level in one go,
 * so continuing a run on a planet's last question cannot pick up a gold.
 *
 * Finishing a planet unlocks its ship paint, its engine trail, its codex card
 * and its hull badge; gold also unlocks the Gilded version of its paint.
 *
 * Saved as "medal.<planet index>=GOLD" and so on.
 */
class PlanetRecords {

    enum Medal {
        NONE("None", new Color(90, 96, 110)),
        BRONZE("Bronze", new Color(205, 127, 50)),
        SILVER("Silver", new Color(200, 208, 220)),
        GOLD("Gold", new Color(255, 205, 60));

        final String label;
        final Color color;

        Medal(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        Medal better(Medal other) { return other.ordinal() > ordinal() ? other : this; }
    }

    static final double GOLD_ACCURACY = 0.9, SILVER_ACCURACY = 0.75;
    static final int SILVER_HEARTS_LOST = 1;

    final Medal[] best = new Medal[Planet.ALL.size()];

    PlanetRecords() { Arrays.fill(best, Medal.NONE); }

    // ---- the planet being flown in the current run

    /** The planet this run is on and being judged for, or -1 at the star or before a run. */
    int tracking = -1;
    /** Whether the run met this planet at its first level; silver and gold need it. */
    boolean fromStart;
    int heartsLost, answered, firstTry;

    /** Begins judging a planet afresh, as a run starts on it or flies into it. */
    void startPlanet(int planet, boolean fromItsStart) {
        tracking = planet;
        fromStart = fromItsStart;
        heartsLost = 0;
        answered = 0;
        firstTry = 0;
    }

    /** A question answered and moved past, and whether it went right the first time. */
    void recordAnswer(boolean rightFirstTry) {
        answered++;
        if (rightFirstTry) firstTry++;
    }

    void recordHeartLost() { heartsLost++; }

    /** First-try accuracy on this planet so far; a clean slate counts as perfect, so a fresh planet reads as gold pace. */
    double accuracy() { return answered == 0 ? 1 : firstTry / (double) answered; }

    /** The medal the planet being flown would earn if it were finished now. */
    Medal judge() {
        if (fromStart && heartsLost == 0 && accuracy() >= GOLD_ACCURACY) return Medal.GOLD;
        if (fromStart && heartsLost <= SILVER_HEARTS_LOST && accuracy() >= SILVER_ACCURACY) return Medal.SILVER;
        return Medal.BRONZE;
    }

    /**
     * A planet has just been finished. Records the medal, keeping the better of
     * it and any earlier one, and returns what this run earned. A planet the
     * run was not judging (which should not happen) still counts, as bronze.
     */
    Medal finishPlanet(int planet) {
        Medal earned = planet == tracking ? judge() : Medal.BRONZE;
        best[planet] = best[planet].better(earned);
        return earned;
    }

    // ---- what has been earned

    /**
     * Whether a planet counts as finished. A medal says so, and so does
     * having flown past its last level: a save from before medals existed,
     * or a practice run, still opens up what the player has travelled through.
     */
    boolean finished(int planet, int highestLevelReached) {
        return best[planet] != Medal.NONE || highestLevelReached > RocketLearn.planetEndLevel(planet);
    }

    boolean gold(int planet) { return best[planet] == Medal.GOLD; }

    /** The medal to show: the one earned, or bronze for a planet passed without one. */
    Medal shown(int planet, int highestLevelReached) {
        return best[planet] != Medal.NONE ? best[planet]
            : finished(planet, highestLevelReached) ? Medal.BRONZE : Medal.NONE;
    }

    int finishedCount(int highestLevelReached) {
        int n = 0;
        for (int p = 0; p < best.length; p++) if (finished(p, highestLevelReached)) n++;
        return n;
    }

    /** How to do better than the medal held, for Level Select. */
    static String nextGoal(Medal held) {
        return switch (held) {
            case NONE -> "Finish this planet to earn a medal";
            case BRONZE -> "Silver: fly it from its first level, lose at most 1 heart, 75% right first try";
            case SILVER -> "Gold: fly it from its first level, lose no hearts, 90% right first try";
            case GOLD -> "The top medal - the Gilded paint is yours";
        };
    }

    void load(Properties p) {
        Arrays.fill(best, Medal.NONE);
        for (int i = 0; i < best.length; i++) {
            String saved = p.getProperty("medal." + i, "").trim();
            for (Medal m : Medal.values()) if (m.name().equalsIgnoreCase(saved)) best[i] = m;
        }
    }

    void save(Properties p) {
        for (int i = 0; i < best.length; i++) {
            if (best[i] != Medal.NONE) p.setProperty("medal." + i, best[i].name());
        }
    }

    // ---- the codex

    /**
     * Two more facts for each planet's codex card, beyond the one shown on
     * arrival, indexed like Planet.ALL.
     */
    static final String[][] CODEX_FACTS = {
        { "A year on Mercury lasts just 88 Earth days.",
          "With almost no air to hold heat, its nights fall to about -180 C." },
        { "Venus spins the opposite way to most planets, so its Sun rises in the west.",
          "A day on Venus (243 Earth days) is longer than its year (225 days)." },
        { "Earth is the densest planet in the solar system.",
          "The Moon drifts about 3.8 cm further from Earth every year." },
        { "Mars has two small moons, Phobos and Deimos.",
          "A day on Mars is only about 40 minutes longer than a day on Earth." },
        { "The Great Red Spot is a storm wider than the whole Earth.",
          "Jupiter spins faster than any other planet: a day there lasts under 10 hours." },
        { "Saturn is less dense than water - it would float in a big enough bath.",
          "Its moon Titan has a thick atmosphere and lakes of liquid methane." },
        { "Uranus was the first planet found with a telescope, by William Herschel in 1781.",
          "Each of its poles gets about 42 years of sunlight, then 42 years of night." },
        { "Neptune was found by maths first: astronomers predicted it from its pull on Uranus.",
          "One year on Neptune lasts about 165 Earth years." },
    };
}
