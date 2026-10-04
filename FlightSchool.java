package rocketlearn;

import java.awt.*;
import java.awt.event.KeyEvent;

import static rocketlearn.RocketLearn.*;

/**
 * A short guided first flight: steer, shoot, answer one easy question,
 * boost and slow, and a word about hints and power-ups. Each step finishes
 * when the player has done it, not on a timer, and nothing can go wrong:
 * it runs under practice rules, so a miss only explains itself.
 *
 * Offered on the main menu until it has been flown once, and always from
 * the How to Play screen (F).
 */
class FlightSchool {

    final RocketLearn game;

    FlightSchool(RocketLearn game) { this.game = game; }

    enum Step { STEER, SHOOT, ANSWER, THRUST, TIPS, DONE }

    Step step = Step.STEER;
    boolean wentLeft, wentRight, boosted, slowed;

    /** The one question: easy on purpose, so the lesson is the flying. */
    static final Question QUESTION = new Question("Shoot the right answer:  2 + 3 = ?",
        new String[]{ "5", "4", "6", "23" }, 0,
        "2 + 3 is 5. Answers ride on the asteroids - shoot the right one, or fly into it.", QuestionBank.BASICS);

    void start() {
        step = Step.STEER;
        wentLeft = wentRight = boosted = slowed = false;
    }

    /** True on the step with an asteroid field to answer. */
    boolean asking() { return step == Step.ANSWER; }

    /** Runs every playing tick before anything else moves. */
    void tick() {
        // Between questions the clock waits and nothing falls.
        if (!asking()) {
            game.timeLeft = game.maxTime;
            game.timeExpired = false;
            game.options.clear();
            game.powerUps.clear();
            game.powerUpSpawnTimer = -1;
        }
        if (game.bannerTimer > 0) return;
        switch (step) {
            case STEER -> {
                if (game.rocketX < GAME_WIDTH * 0.33) wentLeft = true;
                if (game.rocketX > GAME_WIDTH * 0.67) wentRight = true;
                if (wentLeft && wentRight) advance("NICE FLYING!", Step.SHOOT);
            }
            case THRUST -> {
                if (game.throttle > 0.9) boosted = true;
                if (game.throttle < -0.9) slowed = true;
                if (boosted && slowed) advance("THRUSTERS WORKING!", Step.TIPS);
            }
            case TIPS -> {
                game.showBanner("TWO MORE THINGS", new Color(120, 220, 255), 3.0, BannerAction.NONE, true,
                    "Miss a question twice and the right answer glows - with half points.\n"
                    + "Shoot power-ups too: a Shield absorbs a mistake, Freeze stops the clock, 50/50 removes two wrong answers.");
                game.bannerAwaitInput = true;
                step = Step.DONE;
            }
            case DONE -> finish();
            default -> { }
        }
    }

    void shotFired() {
        if (step == Step.SHOOT && game.bannerTimer <= 0) {
            step = Step.ANSWER;
            game.showBanner("GOOD SHOT!", new Color(80, 220, 100), 1.2, BannerAction.NEXT_QUESTION, true,
                "Now answer a question: shoot the asteroid with the right answer");
        }
    }

    /** The right asteroid was hit. */
    void answeredRight() {
        game.options.clear();
        game.bullets.clear();
        game.playSfx(Sfx.CORRECT);
        game.missCount = 0;
        game.repeatQuestion = false;
        advance("CORRECT!", Step.THRUST);
    }

    private void advance(String cheer, Step next) {
        step = next;
        game.playSfx(Sfx.CONFIRM);
        game.showBanner(cheer, new Color(80, 220, 100), 1.2, BannerAction.NONE, true, null);
    }

    private void finish() {
        game.flightSchoolDone = true;
        game.saveFile.saveProgress();
        game.playSfx(Sfx.VICTORY_SOUND);
        game.showBanner("FLIGHT SCHOOL COMPLETE", new Color(255, 215, 90), 2.4, BannerAction.GAME_OVER, true,
            "You're ready. Start a New Game from the menu - good luck out there!");
        game.bannerAwaitInput = true;
        step = Step.STEER;   // so the next tick does not finish twice
        game.flightSchoolRun = false;
        game.practiceMode = true;
    }

    /** What to do now, for the instruction panel. */
    String instruction() {
        return switch (step) {
            case STEER -> "Steer left and right with " + KeyEvent.getKeyText(game.keyLeft) + " and " + KeyEvent.getKeyText(game.keyRight)
                + " (or the arrow keys)" + (wentLeft ? "  -  now the other way" : wentRight ? "  -  now the other way" : "");
            case SHOOT -> "Press " + KeyEvent.getKeyText(game.keyShoot) + " to shoot";
            case ANSWER -> "Shoot the asteroid with the right answer - or fly into it";
            case THRUST -> "Hold " + KeyEvent.getKeyText(game.keyBoost) + " to boost" + (boosted ? " (done)" : "")
                + ", then " + KeyEvent.getKeyText(game.keySlow) + " to slow down" + (slowed ? " (done)" : "");
            default -> "";
        };
    }

    String why() {
        return switch (step) {
            case THRUST -> "Boost: rocks fall faster, answers score more.  Slow: rocks ease off, but the timer runs faster.";
            case ANSWER -> "Each question shows how long you have. A wrong answer here only explains itself.";
            default -> null;
        };
    }

    int stepNumber() { return Math.min(step.ordinal() + 1, 5); }

    /** The instruction panel across the top of the playfield. */
    void draw(Graphics2D g2) {
        if (step == Step.DONE || step == Step.TIPS) return;
        String text = instruction(), sub = why();
        int w = 700, h = sub == null ? 66 : 88, x = GAME_WIDTH / 2 - w / 2, y = 96;
        g2.setColor(new Color(8, 14, 28, 215));
        g2.fillRoundRect(x, y, w, h, 16, 16);
        g2.setColor(new Color(120, 220, 255, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 16, 16);
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(font(Font.BOLD, 13));
        g2.setColor(new Color(120, 220, 255));
        game.drawCentered(g2, "FLIGHT SCHOOL  -  STEP " + stepNumber() + " OF 5", GAME_WIDTH / 2, y + 20);
        g2.setFont(font(Font.BOLD, game.textSize(19)));
        g2.setColor(Color.WHITE);
        game.drawCentered(g2, text, GAME_WIDTH / 2, y + 46);
        if (sub != null) {
            g2.setFont(font(Font.PLAIN, 14));
            g2.setColor(new Color(185, 200, 220));
            game.drawCentered(g2, sub, GAME_WIDTH / 2, y + 72);
        }
    }
}
