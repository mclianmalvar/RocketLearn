package rocketlearn;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.*;

import static rocketlearn.RocketLearn.*;

/**
 * The end of the journey: the star's speech before and after its fifty
 * questions, the JOURNEY COMPLETE screen, and the run report on it.
 */
class StarEnding {

    final RocketLearn game;

    StarEnding(RocketLearn game) { this.game = game; }

    static final String[] STAR_INTRO_LINES = {
        "Now that you get close to me, let's summarize",
        "if you really learn from your way out here!",
        "",
        "Fifty questions. Everything you met on the way -",
        "easy, medium, hard, in no order at all.",
        "",
        "Show me what stayed with you."
    };

    static final String[] STAR_LINES = {
        "Enough. You beat me.",
        "Eight worlds behind you, and every answer earned.",
        "Everything out here is lit by what I burn - and you",
        "learned your way to it, one question at a time.",
        "",
        "Go well, traveller. You earned the light."
    };

    String[] starLines() { return game.starIntro ? STAR_INTRO_LINES : STAR_LINES; }

    /**
     * The ending. The star fills the screen and speaks a line at a time, then
     * asks whether the player wants to keep learning or call it a journey.
     */
    void drawStarScreen(Graphics2D g2) {
        g2.setColor(new Color(8, 6, 14, 225));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        double pulse = 0.5 + 0.5 * Math.sin(game.flameFlicker * 0.7);
        double cx = GAME_WIDTH / 2.0, cy = 118;
        double r = 46 + pulse * 6;

        Paint old = g2.getPaint();
        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(cx, cy), (float) (r * 6),
            new float[]{0f, 0.3f, 1f},
            new Color[]{
                new Color(255, 225, 140, 110),
                new Color(255, 190, 80, 45),
                new Color(255, 170, 60, 0)
            }));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        g2.setPaint(old);

        AffineTransform oldTx = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(game.flameFlicker * 0.015);
        for (int i = 0; i < 16; i++) {
            double ang = i * Math.PI / 8;
            double len = r * (i % 2 == 0 ? 2.3 : 1.6) + pulse * 10;
            Path2D ray = new Path2D.Double();
            ray.moveTo(Math.cos(ang - 0.06) * r, Math.sin(ang - 0.06) * r);
            ray.lineTo(Math.cos(ang) * len, Math.sin(ang) * len);
            ray.lineTo(Math.cos(ang + 0.06) * r, Math.sin(ang + 0.06) * r);
            ray.closePath();
            g2.setColor(new Color(255, 218, 125, 55));
            g2.fill(ray);
        }
        g2.setTransform(oldTx);

        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(cx - r * 0.2, cy - r * 0.2), (float) r,
            new float[]{0f, 0.6f, 1f},
            new Color[]{ Color.WHITE, new Color(255, 238, 168), new Color(255, 180, 62) }));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.setPaint(old);

        game.scenery.drawStarFace(g2, cx, cy, r, starFinishedSpeaking() ? FACE_CALM : FACE_SPEAKING, -1);

        // The star's words, revealed a line at a time.
        String[] lines = starLines();
        g2.setFont(font(Font.PLAIN, 18));
        FontMetrics fm = g2.getFontMetrics();
        int y = 214;
        for (int i = 0; i < lines.length && i <= game.starLineIndex; i++) {
            int alpha = i < game.starLineIndex ? 235
                : (int) (235 * clamp(game.starReveal / STAR_LINE_FADE, 0, 1));
            g2.setColor(new Color(255, 240, 205, Math.max(0, alpha)));
            game.drawCentered(g2, lines[i], GAME_WIDTH / 2, y);
            y += fm.getHeight() + 1;
        }

        if (!starFinishedSpeaking()) {
            float blink = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2.5));
            g2.setFont(font(Font.PLAIN, 13));
            g2.setColor(new Color(200, 200, 215, (int) (90 + blink * 110)));
            game.drawCentered(g2, "Press " + KeyEvent.getKeyText(game.keyShoot) + " or click to listen on", GAME_WIDTH / 2, GAME_HEIGHT - 28);
            return;
        }

        float blink = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2.2));
        g2.setFont(font(Font.BOLD, 24));
        g2.setColor(new Color(255, 225, 110, (int) (165 + blink * 90)));
        if (game.starIntro) {
            game.drawCentered(g2, "Press " + KeyEvent.getKeyText(game.keyShoot) + " or click to face the star", GAME_WIDTH / 2, 412);
            g2.setFont(font(Font.PLAIN, 14));
            g2.setColor(new Color(200, 190, 175));
            game.drawCentered(g2, "Its flares will not wait for you.", GAME_WIDTH / 2, 446);
        } else {
            game.drawCentered(g2, "Press " + KeyEvent.getKeyText(game.keyShoot) + " or click to finish the journey", GAME_WIDTH / 2, 412);
        }
    }

    static final double STAR_LINE_FADE = 0.45;

    boolean starFinishedSpeaking() {
        return game.starLineIndex >= starLines().length - 1 && game.starReveal >= STAR_LINE_FADE;
    }

    /** Reveals the star's next line, or jumps straight to the end of the speech. */
    void advanceStarSpeech() {
        if (starFinishedSpeaking()) return;
        if (game.starReveal < STAR_LINE_FADE) {
            game.starReveal = STAR_LINE_FADE;
        } else if (game.starLineIndex < starLines().length - 1) {
            game.starLineIndex++;
            game.starReveal = 0;
        }
        game.playSfx(Sfx.CLICK);
    }

    void updateStarScreen(double dt) {
        if (starFinishedSpeaking()) return;
        String[] lines = starLines();
        game.starReveal += dt;
        if (game.starReveal >= STAR_LINE_FADE && game.starLineIndex < lines.length - 1) {
            // Blank lines are only spacing, so they pass straight through.
            if (lines[game.starLineIndex].isEmpty() || game.starReveal >= STAR_LINE_FADE + 0.9) {
                game.starLineIndex++;
                game.starReveal = 0;
            }
        }
    }

    /** Opens the star's screen, either to introduce the final test or to close it. */
    void openStarScreen(boolean intro) {
        game.starIntro = intro;
        game.starLineIndex = 0;
        game.starReveal = 0;
        game.state = State.STAR;
        game.syncMusic();
    }

    void drawVictoryScreen(Graphics2D g2) {
        // Near-opaque: this is an ending, not an overlay on a paused game.
        g2.setColor(new Color(10, 5, 25, 238));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.2));
        Paint oldPaint = g2.getPaint();
        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(GAME_WIDTH / 2.0, GAME_HEIGHT / 2.0 - 60), 300f,
            new float[]{0f, 1f},
            new Color[]{ new Color(255, 215, 0, (int) (70 + pulse * 40)), new Color(255, 215, 0, 0) }));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        g2.setPaint(oldPaint);

        g2.setColor(new Color(255, 215, 0));
        g2.setFont(font(Font.BOLD, 40));
        game.drawCentered(g2, "JOURNEY COMPLETE", GAME_WIDTH / 2, 62);

        g2.setColor(new Color(190, 230, 255));
        g2.setFont(font(Font.PLAIN, 16));
        game.drawCentered(g2, "Eight worlds, then the star. You made it all the way.", GAME_WIDTH / 2, 88);

        drawRunReport(g2);

        game.drawSelectableList(g2, game.victoryItems, game.victoryIndex, 392, 34, 22, 18);

        // The run that earned Starborn shows it off beside the choices.
        if (game.starbornNewThisRun) {
            int px = GAME_WIDTH / 2 + 250;
            game.ship.drawShipPreview(g2, ShipSkin.STARBORN, px, 418, 1.15);
            game.drawNewBadge(g2, px - 17, 365);
            g2.setFont(font(Font.BOLD, 14));
            g2.setColor(new Color(255, 215, 90));
            game.drawCentered(g2, "Starborn paint unlocked", px, 476);
            g2.setFont(font(Font.PLAIN, 11));
            g2.setColor(new Color(170, 165, 150));
            game.drawCentered(g2, "Choose it in Settings > Ship", px, 491);
        }

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 13));
        game.drawCentered(g2, "W/S or Up/Down to choose, ENTER to select", GAME_WIDTH / 2, GAME_HEIGHT - 16);
    }

    /**
     * The send-off: what the run actually amounted to. Everything here is
     * already tracked while playing, it just had nowhere to be shown.
     */
    void drawRunReport(Graphics2D g2) {
        int panelW = 660, panelX = GAME_WIDTH / 2 - panelW / 2, panelY = 104, panelH = 258;
        g2.setColor(new Color(10, 8, 22, 215));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 16, 16);
        g2.setColor(new Color(255, 215, 0, 110));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 16, 16);

        // Headline figures across the top.
        int acc = game.runAccuracy();
        String[] labels = { "FINAL SCORE", "ACCURACY", "LEVELS FLOWN" };
        String[] values = { String.valueOf(game.score), acc + "%", String.valueOf(Math.max(0, game.level - game.gameStartLevel)) };
        Color[] tints = {
            new Color(255, 225, 120),
            acc >= 80 ? new Color(90, 225, 120) : acc >= 60 ? Color.ORANGE : new Color(235, 110, 100),
            new Color(150, 205, 255)
        };
        int cellW = panelW / 3;
        for (int i = 0; i < labels.length; i++) {
            int cx = panelX + cellW * i + cellW / 2;
            g2.setFont(font(Font.PLAIN, 11));
            g2.setColor(new Color(150, 160, 185));
            game.drawCentered(g2, labels[i], cx, panelY + 28);
            g2.setFont(font(Font.BOLD, 34));
            g2.setColor(tints[i]);
            game.drawCentered(g2, values[i], cx, panelY + 66);
        }

        g2.setColor(new Color(255, 215, 0, 60));
        g2.drawLine(panelX + 24, panelY + 86, panelX + panelW - 24, panelY + 86);

        int y = panelY + 116;
        g2.setFont(font(Font.PLAIN, 15));

        g2.setColor(new Color(215, 222, 238));
        game.drawCentered(g2, game.runCorrect + " of " + game.runAnswered + " questions answered first time", GAME_WIDTH / 2, y);
        y += 26;

        String weak = game.weakestTopic();
        if (weak == null) {
            g2.setColor(new Color(110, 235, 150));
            g2.setFont(font(Font.BOLD, 16));
            game.drawCentered(g2, "Not one mistake the whole way out.", GAME_WIDTH / 2, y);
        } else {
            g2.setColor(new Color(235, 150, 130));
            game.drawCentered(g2, "Trickiest topic: " + weak + "  (" + game.weakestTopicMisses() + " missed)", GAME_WIDTH / 2, y);
        }
        y += 26;

        if (!game.missedQuestions.isEmpty()) {
            g2.setColor(new Color(170, 180, 200));
            g2.setFont(font(Font.PLAIN, 14));
            game.drawCentered(g2, game.missedQuestions.size() + " question"
                + (game.missedQuestions.size() == 1 ? "" : "s") + " worth another look", GAME_WIDTH / 2, y);
        }
        y += 30;

        // What the star was told, and what it made of it.
        g2.setFont(font(Font.ITALIC, 15));
        g2.setColor(new Color(255, 226, 150));
        game.drawCentered(g2, game.beyondTheStar
            ? "\"You flew on past me. Good.\""
            : "\"Then rest, traveller. You earned the quiet.\"", GAME_WIDTH / 2, y);
        y += 24;
        g2.setFont(font(Font.PLAIN, 12));
        g2.setColor(new Color(150, 145, 160));
        game.drawCentered(g2, "- The Star", GAME_WIDTH / 2, y);
    }
}
