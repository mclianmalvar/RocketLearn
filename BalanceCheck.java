package rocketlearn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Headless difficulty report.
 *
 * Drives the real game loop with a scripted pilot so the numbers come from the
 * actual physics rather than from guesswork: the same falling speeds, the same
 * timer, the same lane shuffling, wind, gravity and solar flares the player
 * meets. The pilot steers toward an answer and shoots it, occasionally aiming
 * at the wrong one to stand in for a player who is not perfect.
 *
 *   javac -d out rocketlearn\*.java
 *   java -Djava.awt.headless=true -cp out rocketlearn.BalanceCheck
 *
 * It answers one question: is the late game actually survivable?
 */
public class BalanceCheck {

    static final double DT = 1.0 / 60;
    /** A generous ceiling: no single question should take anywhere near this. */
    static final int MAX_FRAMES = 60 * 120;

    /** How reliably the pilot picks the right asteroid. */
    static final double[] SKILLS = { 0.60, 0.75, 0.90 };
    /**
     * Enough runs that the verdict is stable. At 40 the "lost anyway" figure
     * swung by eight points between runs, which is wider than the threshold it
     * is being judged against.
     */
    static final int TRIALS = 120;

    /**
     * Seconds spent reading before the pilot moves. Without this the bot answers
     * in under two seconds, the timer never matters, and the report flatters
     * every level.
     */
    static final double READ_SECONDS = 7.0, READ_JITTER = 4.0;

    /** How one destination held up. */
    static class Result {
        String name;
        /** Cleared it; lost it despite aiming right; lost it by aiming wrong. */
        int cleared, lostFairly, heartsLost;
        double avgSecondsLeft;
    }

    /**
     * Plays a single question at the given level.
     *
     * @return true if the pilot cleared it without losing a heart
     */
    static boolean playQuestion(RocketLearn g, Random rng, double skill,
                                double[] secondsLeftOut, boolean aimTrue) {
        // A real player has to read the question before they can fly at anything.
        double readingLeft = READ_SECONDS + rng.nextDouble() * READ_JITTER;

        // Which asteroid the pilot has decided to go for.
        AnswerOption target = null;
        int startLevel = g.level;
        int startHearts = g.hearts;

        for (int frame = 0; frame < MAX_FRAMES; frame++) {
            // The star interrupts with dialogue; press on through it.
            if (g.state == State.STAR) {
                g.starLineIndex = StarEnding.STAR_INTRO_LINES.length;
                g.starReveal = 10;
                g.input.confirmCurrent();
                continue;
            }
            if (g.state != State.PLAYING) break;

            if (g.bannerTimer > 0 || g.exploding) {
                // An explanation waits for a key press; the pilot presses on.
                if (g.bannerAwaitInput) g.dismissBanner();
                g.leftHeld = g.rightHeld = false;
                g.update(DT);
                if (g.level != startLevel || g.hearts != startHearts) break;
                continue;
            }

            // Still reading: hold position and let the clock run.
            if (readingLeft > 0) {
                readingLeft -= DT;
                g.leftHeld = g.rightHeld = false;
                if (secondsLeftOut != null) secondsLeftOut[0] = g.timeLeft;
                g.update(DT);
                if (g.level != startLevel || g.hearts != startHearts) break;
                continue;
            }

            // Choose a target, and re-choose if lane shuffling took it away.
            if (target == null || !g.options.contains(target)) {
                target = null;
                List<AnswerOption> wrong = new ArrayList<>();
                for (AnswerOption o : g.options) {
                    if (o.correct && aimTrue) target = o;
                    if (!o.correct) wrong.add(o);
                }
                if (target == null && !aimTrue && !wrong.isEmpty()) {
                    target = wrong.get(rng.nextInt(wrong.size()));
                }
                if (target == null && !g.options.isEmpty()) target = g.options.get(0);
            }

            if (target != null) {
                // Steer toward where the rock is settling, not where it is now -
                // that is what a player does while the lanes are shuffling.
                double aimX = Math.abs(target.targetX - target.x) > 4 ? target.targetX : target.x;
                double dx = aimX - g.rocketX;
                g.leftHeld = dx < -10;
                g.rightHeld = dx > 10;
                // An asteroid is CHIP_W wide, so a shot lands from well off-centre.
                // Firing only when perfectly lined up badly understates a human.
                if (Math.abs(target.x - g.rocketX) < RocketLearn.CHIP_W * 0.32 && g.fireCooldown <= 0) {
                    g.shoot();
                }
                // Ease off when the rock is getting low and the shot is not lined
                // up yet. Players do this; leaving it out understates them.
                g.slowHeld = target.y > RocketLearn.GAME_HEIGHT * 0.58
                    && Math.abs(target.x - g.rocketX) > RocketLearn.CHIP_W * 0.32
                    && g.timeLeft > 6;
            }

            if (secondsLeftOut != null) secondsLeftOut[0] = g.timeLeft;
            g.update(DT);

            if (g.level != startLevel || g.hearts != startHearts) break;
        }

        g.leftHeld = g.rightHeld = g.slowHeld = false;
        return g.hearts == startHearts && g.level != startLevel;
    }

    static Result runDestination(String name, int level, double skill, long seed) {
        Result r = new Result();
        r.name = name;
        double secondsTotal = 0;
        // One stream for the whole run. Seeding a fresh Random per trial with
        // consecutive seeds gives correlated first draws, which skewed the
        // low-skill column badly.
        Random rng = new Random(seed);

        for (int trial = 0; trial < TRIALS; trial++) {
            // Seed the game too, not just the pilot. Otherwise the questions,
            // spawn heights, lane swaps and power-ups differ every run and two
            // reports cannot be compared against each other.
            RocketLearn.rngSeed = seed * 1000L + trial;
            RocketLearn g = new RocketLearn();
            g.adaptiveHelp = false;   // measure the game as tuned, without Adaptive Help
            g.timer.stop();
            g.soundOn = false;
            g.startGame(level);

            double[] left = new double[1];
            boolean aimedTrue = rng.nextDouble() < skill;
            boolean ok = playQuestion(g, rng, skill, left, aimedTrue);
            if (ok) {
                r.cleared++;
                secondsTotal += left[0];
            } else if (aimedTrue) {
                // It went for the right answer and still lost it - the asteroid
                // fell past, or the clock ran out. That is a tuning problem.
                r.lostFairly++;
            } else {
                r.heartsLost++;
            }
        }
        r.avgSecondsLeft = r.cleared == 0 ? 0 : secondsTotal / r.cleared;
        return r;
    }

    public static void main(String[] args) {
        new java.io.File("rocketlearn.save").delete();

        String[] names = new String[9];
        int[] levels = new int[9];
        for (int i = 0; i < 8; i++) {
            names[i] = Planet.ALL.get(i).name;
            // A little way into the planet, but never past where the star begins.
            levels[i] = Math.min(RocketLearn.PLANET_START_LEVEL[i] + 6, RocketLearn.STAR_LEVEL - 1);
        }
        names[8] = "The Star";
        levels[8] = RocketLearn.STAR_LEVEL;

        System.out.println("RocketLearn balance report");
        System.out.println("Each cell: % of questions cleared without losing a heart, over "
            + TRIALS + " tries.");
        System.out.println("The pilot aims correctly at the stated skill level.\n");

        System.out.printf("%-10s %-22s", "DESTINATION", "TRAIT");
        for (double s : SKILLS) System.out.printf("  skill %.0f%%", s * 100);
        System.out.println("   lost anyway   timer left");

        List<String> unfair = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Planet p = i == 8 ? Planet.STAR : Planet.ALL.get(i);
            System.out.printf("%-10s %-22s", names[i], p.traitName);
            double lastLeft = 0;
            int lostFairlyPct = 0;
            for (double s : SKILLS) {
                Result r = runDestination(names[i], levels[i], s, 1234L + i * 97);
                System.out.printf("  %8d%%", r.cleared * 100 / TRIALS);
                lastLeft = r.avgSecondsLeft;
                if (s >= 0.90) {
                    // Of the times it aimed right, how often did it still lose?
                    int aimedRight = r.cleared + r.lostFairly;
                    lostFairlyPct = aimedRight == 0 ? 0 : r.lostFairly * 100 / aimedRight;
                }
            }
            System.out.printf("   %10d%%   %6.1fs%n", lostFairlyPct, lastLeft);
            // Losing while aiming correctly is the level's fault, not the player's.
            if (lostFairlyPct > 25) unfair.add(names[i] + " (" + lostFairlyPct + "%)");
        }

        System.out.println();
        System.out.println("\"lost anyway\" = went for the correct answer and still lost a heart,");
        System.out.println("because it fell past or the clock beat them. High values mean unfair tuning.");
        System.out.println();
        if (unfair.isEmpty()) {
            System.out.println("PASS: every destination is fair to a pilot who knows the answer.");
        } else {
            System.out.println("WARNING: too punishing even when the answer is known - " + String.join(", ", unfair));
        }
        RocketLearn.rngSeed = 0;
        new java.io.File("rocketlearn.save").delete();
        System.exit(0);
    }
}
