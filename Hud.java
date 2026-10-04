package rocketlearn;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import static rocketlearn.RocketLearn.*;

/**
 * Everything drawn over the play while flying: score, hearts and timer,
 * the question panel, banners, the star's health bar and its taunts.
 */
class Hud {

    final RocketLearn game;

    Hud(RocketLearn game) { this.game = game; }

    /**
     * The star's gauntlet bar. Fifty questions is far too many to track from a
     * level number, so it reads like a boss meter being worn down.
     */
    void drawBossBar(Graphics2D g2) {
        int barW = 400, barH = 15;
        int barX = GAME_WIDTH / 2 - barW / 2, barY = 24;
        double done = (game.starQuestionNumber() - 1) / (double) STAR_QUESTIONS;
        double charge = game.starFlareCharge > 0 ? 1 - clamp(game.starFlareCharge / FLARE_CHARGE_TIME, 0, 1) : 0;
        float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.6));

        g2.setFont(font(Font.BOLD, 12));
        g2.setColor(new Color(255, 206, 110, 225));
        game.drawCentered(g2, "T H E   S T A R", GAME_WIDTH / 2, barY - 6);

        // A flare gathering makes the whole meter glow.
        if (charge > 0 || game.starFlaring()) {
            Paint old = g2.getPaint();
            double heat = Math.max(charge, game.starFlaring() ? 1 : 0);
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0, barY + barH / 2.0), barW * 0.62f,
                new float[]{0f, 1f},
                new Color[]{ new Color(255, 140, 40, (int) (120 * heat)), new Color(255, 120, 30, 0) }));
            g2.fillRect(barX - 60, barY - 22, barW + 120, barH + 44);
            g2.setPaint(old);
        }

        g2.setColor(new Color(26, 10, 6, 235));
        g2.fillRoundRect(barX, barY, barW, barH, 7, 7);

        Paint old = g2.getPaint();
        g2.setPaint(new GradientPaint(
            barX, barY, new Color(255, 236, 160),
            barX, barY + barH, new Color(226, 120, 30)));
        g2.fillRoundRect(barX, barY, (int) (barW * done), barH, 7, 7);
        g2.setPaint(old);

        // Tick marks every ten so progress is countable at a glance.
        g2.setColor(new Color(20, 8, 4, 170));
        for (int i = 10; i < STAR_QUESTIONS; i += 10) {
            int tx = barX + (int) (barW * (i / (double) STAR_QUESTIONS));
            g2.drawLine(tx, barY + 2, tx, barY + barH - 2);
        }

        g2.setColor(new Color(255, 214, 120, (int) (185 + pulse * 70)));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(barX, barY, barW, barH, 7, 7);
        g2.setStroke(new BasicStroke(1f));

        // Count sits inside the bar - to the right it collided with the score.
        g2.setFont(font(Font.BOLD, 11));
        g2.setColor(new Color(0, 0, 0, 170));
        game.drawCentered(g2, game.starQuestionNumber() + " / " + STAR_QUESTIONS, GAME_WIDTH / 2 + 1, barY + barH - 3);
        g2.setColor(Color.WHITE);
        game.drawCentered(g2, game.starQuestionNumber() + " / " + STAR_QUESTIONS, GAME_WIDTH / 2, barY + barH - 4);
    }

    /**
     * What the star is saying, in a bubble tucked between the HUD and the
     * question panel with its tail pointing back at the face.
     */
    void drawStarTaunt(Graphics2D g2) {
        if (game.starTaunt == null || game.starTauntTimer <= 0) return;

        // Fade in quickly, hold, then fade out.
        double t = clamp(game.starTauntTimer / TAUNT_TIME, 0, 1);
        float alpha = (float) clamp(Math.min((1 - t) * 6, t * 3), 0, 1);

        g2.setFont(font(Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(game.starTaunt);
        int boxW = textW + 30, boxH = fm.getHeight() + 14;
        int boxX = GAME_WIDTH - 118 - boxW, boxY = 116;

        g2.setColor(new Color(22, 10, 4, (int) (232 * alpha)));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Tail, pointing toward the star's face.
        Path2D tail = new Path2D.Double();
        tail.moveTo(boxX + boxW - 4, boxY + boxH * 0.35);
        tail.lineTo(boxX + boxW + 18, boxY + boxH * 0.62);
        tail.lineTo(boxX + boxW - 4, boxY + boxH * 0.78);
        tail.closePath();
        g2.fill(tail);

        g2.setColor(new Color(255, 206, 110, (int) (200 * alpha)));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g2.draw(tail);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(255, 238, 200, (int) (255 * alpha)));
        game.drawCentered(g2, game.starTaunt, boxX + boxW / 2, boxY + fm.getAscent() + 7);
    }

    void drawHUD(Graphics2D g2) {
        if (game.atStar() && !game.pastTheGauntlet() && !game.practiceMode) drawBossBar(g2);
        drawStarTaunt(g2);

        if (game.practiceMode) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.2));
            g2.setFont(font(Font.BOLD, 17));
            g2.setColor(new Color(120, 220, 255, (int) (190 + pulse * 65)));
            g2.drawString(game.flightSchoolRun ? "FLIGHT SCHOOL" : game.dailyRun ? "DAILY FLIGHT" : game.mistakesDrill ? "MISTAKES DRILL" : "PRACTICE", 20, 38);
        } else {
            g2.setFont(font(Font.BOLD, 30));
            for (int i = 0; i < game.startingHearts; i++) {
                boolean alive = i < game.hearts;
                double hx = 20 + i * 36, hy = 40;

                // The heart just lost punches outward as it greys.
                if (i == game.lostHeartIndex && game.heartPunch > 0) {
                    double p = clamp(game.heartPunch / 0.5, 0, 1);
                    AffineTransform oldTx = g2.getTransform();
                    g2.translate(hx + 9, hy - 9);
                    double s = 1 + p * 0.9;
                    g2.scale(s, s);
                    g2.setColor(new Color(255, 80, 100, (int) (p * 235)));
                    g2.drawString("♥", -9, 9);
                    g2.setTransform(oldTx);
                    continue;
                }
                g2.setColor(alive ? Color.RED : new Color(70, 70, 70));
                g2.drawString("♥", (int) hx, (int) hy);
            }
        }

        // Score rolls up to its new value and pops briefly on a gain.
        if (!game.practiceMode) {
            AffineTransform oldTx = g2.getTransform();
            double pop = 1 + clamp(game.scorePop / 0.45, 0, 1) * 0.28;
            g2.translate(GAME_WIDTH - 210, 30);
            g2.scale(pop, pop);
            g2.setColor(game.scorePop > 0 ? new Color(150, 255, 175) : Color.WHITE);
            g2.setFont(font(Font.BOLD, 18));
            g2.drawString("Score: " + (int) Math.round(game.displayScore), 0, 0);
            g2.setTransform(oldTx);

            g2.setColor(new Color(170, 178, 195));
            g2.setFont(font(Font.PLAIN, 12));
            g2.drawString("Best: " + game.highScore, GAME_WIDTH - 210, 47);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.BOLD, 18));
        String levelText = "Level: " + game.level + " ";
        g2.drawString(levelText, GAME_WIDTH - 210, 70);
        FontMetrics lfm = g2.getFontMetrics();
        int levelTextW = lfm.stringWidth(levelText);
        g2.setColor(game.difficultyColor());
        g2.setFont(font(Font.BOLD, 12));
        g2.drawString(game.difficultyName(), GAME_WIDTH - 210 + levelTextW, 70);

        g2.setColor(new Color(150, 200, 230));
        g2.setFont(font(Font.PLAIN, 12));
        g2.drawString(planetProgressText(), GAME_WIDTH - 210, 87);

        int rightY = 104;
        if (isShuffleZone(game.level)) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.5));
            g2.setColor(new Color(255, 90, 220, (int) (170 + pulse * 85)));
            g2.setFont(font(Font.BOLD, 12));
            g2.drawString("LANES SHUFFLING", GAME_WIDTH - 210, rightY);
            rightY += 16;
        }

        if (game.throttle > 0.02) {
            int alpha = (int) (60 + game.throttle * 195);
            g2.setColor(new Color(120, 220, 255, alpha));
            g2.setFont(font(Font.BOLD, 12));
            g2.drawString(String.format("BOOST %.0f%%  -  x%.1f SCORE", game.throttle * 100, 1.0 + game.throttle), GAME_WIDTH - 210, rightY);
        } else if (game.throttle < -0.02) {
            double slowAmt = -game.throttle;
            int alpha = (int) (60 + slowAmt * 195);
            g2.setColor(new Color(255, 210, 100, alpha));
            g2.setFont(font(Font.BOLD, 12));
            g2.drawString(String.format("SLOW %.0f%%  -  TIMER x%.1f", slowAmt * 100, 1.0 + slowAmt), GAME_WIDTH - 210, rightY);
        }

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 13));
        g2.drawString("ESC - Pause  •  TAB - " + (game.showQuestionPanel ? "Hide" : "Show") + " Question", 20, 62);

        int leftY = 80;
        if (game.practiceMode && game.practiceFocus != null) {
            g2.setColor(new Color(160, 205, 235));
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString("Focus: " + game.practiceFocus, 20, leftY);
            leftY += 18;
        }
        if (game.dailyRun) {
            g2.setColor(new Color(255, 215, 120));
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString("Question " + Math.min(game.daily.asked + 1, game.daily.questions.size()) + " of " + game.daily.questions.size()
                + "  •  " + game.daily.rightFirstTry + " right first try", 20, leftY);
            leftY += 18;
        }
        if (game.mistakesDrill) {
            g2.setColor(new Color(160, 205, 235));
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString(game.notebook.practiceCount() + " left to go", 20, leftY);
            leftY += 18;
        }
        // The teacher's topic for this planet, when questions.txt sets one.
        String planetFocus = game.practiceMode ? null : game.dealer.planetFocus();
        if (planetFocus != null) {
            g2.setColor(new Color(160, 205, 235));
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString("Topic focus: " + planetFocus, 20, leftY);
            leftY += 18;
        }
        if (game.inChallenge()) {
            g2.setColor(new Color(255, 215, 90));
            g2.setFont(font(Font.BOLD, 13));
            g2.drawString("MEDAL CHALLENGE  -  best here: "
                + game.planetRecords.shown(game.challengePlanet, game.highestLevelReached).label, 20, leftY);
            leftY += 18;
        }
        // The medal this planet is on course for, so it can be chased while flying.
        PlanetRecords pr = game.planetRecords;
        if (!game.practiceMode && !game.atStar() && pr.tracking == game.planetIndex()) {
            PlanetRecords.Medal pace = pr.judge();
            MenuScreens.drawMedal(g2, pace, 25, leftY - 5, 5);
            g2.setColor(pace.color);
            g2.setFont(font(Font.BOLD, 13));
            String detail = !pr.fromStart ? "joined part way"
                : pr.answered == 0 && pr.heartsLost == 0 ? "fresh start"
                : pr.heartsLost + (pr.heartsLost == 1 ? " heart" : " hearts") + " lost  •  "
                    + Math.round(pr.accuracy() * 100) + "% first try";
            g2.drawString(pace.label + " pace  -  " + detail, 36, leftY);
            leftY += 18;
        }
        if (game.assistReason != null) {
            g2.setColor(new Color(170, 230, 170));
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString(game.assistReason, 20, leftY);
            leftY += 18;
        }
        if (!game.practiceMode && game.hearts < game.startingHearts) {
            int toHeal = HEAL_STREAK - (game.correctStreak % HEAL_STREAK);
            g2.setColor(new Color(255, 150, 170));
            g2.drawString(toHeal + " correct to heal a heart", 20, leftY);
            leftY += 18;
        }

        if (game.hasShield) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2));
            g2.setColor(new Color(90, 200, 255, (int) (180 + pulse * 75)));
            g2.setFont(font(Font.BOLD, 13));
            g2.drawString("SHIELD READY - absorbs one mistake", 20, leftY);
            leftY += 18;
        }

        if (game.freezeTimer > 0) {
            g2.setColor(new Color(160, 230, 255));
            g2.setFont(font(Font.BOLD, 13));
            g2.drawString(String.format("FROZEN %.1fs", game.freezeTimer), 20, leftY);
        }

        if (game.toastTimer > 0 && game.toastText != null) {
            float fade = (float) clamp(game.toastTimer / 0.5, 0, 1);
            g2.setFont(font(Font.BOLD, 17));
            FontMetrics tfm = g2.getFontMetrics();
            int tw = tfm.stringWidth(game.toastText) + 30;
            int ty = GAME_HEIGHT - 70;
            g2.setColor(new Color(8, 10, 16, (int) (185 * fade)));
            g2.fillRoundRect(GAME_WIDTH / 2 - tw / 2, ty - tfm.getAscent() - 8, tw, tfm.getHeight() + 12, 12, 12);
            g2.setColor(new Color(game.toastColor.getRed(), game.toastColor.getGreen(), game.toastColor.getBlue(), (int) (255 * fade)));
            g2.drawRoundRect(GAME_WIDTH / 2 - tw / 2, ty - tfm.getAscent() - 8, tw, tfm.getHeight() + 12, 12, 12);
            game.drawCentered(g2, game.toastText, GAME_WIDTH / 2, ty);
        }
    }

    String planetProgressText() {
        if (game.atStar()) {
            if (game.beyondTheStar || game.pastTheGauntlet()) return "Flying on with the star";
            return "Question " + game.starQuestionNumber() + " of " + STAR_QUESTIONS;
        }
        int next = game.nextPlanetStartLevel();
        if (next == -1) return (STAR_LEVEL - game.level) + " to The Star";
        return (next - game.level) + " to " + planets.get(planetIndexForLevel(next)).name;
    }

    void drawQuestionPanel(Graphics2D g2) {
        g2.setFont(font(Font.BOLD, game.textSize(24)));
        FontMetrics qfm = g2.getFontMetrics();
        List<String> qLines = game.wrapText(qfm, game.questionText, 640);
        int qLineH = qfm.getHeight();
        int missLineH = game.missCount > 0 ? 24 : 0;
        int barW = 300, barH = 14, barGap = 18;

        String[] codeLines = game.currentQuestion == null ? new String[0] : game.currentQuestion.codeLines();
        int codeGap = codeLines.length == 0 ? 0 : 12;
        int codeH = 0;
        if (codeLines.length > 0) {
            FontMetrics cfm = g2.getFontMetrics(monoFont(Font.PLAIN, game.codeFontSize(g2, codeLines, 600)));
            codeH = codeLines.length * cfm.getHeight() + 20;
        }

        // While the right answer glows, its explanation stays up too, so the
        // player reads why while flying at it rather than only beforehand.
        Font whyFont = font(Font.PLAIN, game.textSize(14));
        FontMetrics wfm = g2.getFontMetrics(whyFont);
        List<String> whyLines = game.hintActive() && game.explanationsOn() && game.currentQuestion != null
            ? game.wrapText(wfm, "Why: " + game.currentQuestion.why, 620) : List.of();
        int whyH = whyLines.isEmpty() ? 0 : whyLines.size() * wfm.getHeight() + 6;

        int contentH = qLines.size() * qLineH + codeGap + codeH + missLineH + whyH + barGap + barH;
        int panelPad = 20;
        int panelW = 680, panelH = contentH + panelPad * 2;
        int panelX = GAME_WIDTH / 2 - panelW / 2;
        int panelY = GAME_HEIGHT / 2 - panelH / 2;

        g2.setColor(game.highContrast ? Color.BLACK : new Color(8, 10, 16, 150));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        g2.setColor(new Color(110, 190, 255, 210));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        g2.setStroke(new BasicStroke(1f));

        int qStartY = panelY + panelPad + qfm.getAscent();
        for (int i = 0; i < qLines.size(); i++) {
            game.drawCenteredShadowed(g2, qLines.get(i), GAME_WIDTH / 2, qStartY + i * qLineH, Color.YELLOW);
        }
        int afterQuestionY = qStartY - qfm.getAscent() + qLines.size() * qLineH;
        if (codeLines.length > 0) {
            afterQuestionY += codeGap + game.drawCodeBlock(g2, codeLines, afterQuestionY + codeGap, 600);
        }
        if (game.missCount > 0) {
            g2.setFont(font(Font.BOLD, game.textSize(16)));
            game.drawCenteredShadowed(g2, "Same question - attempt " + (game.missCount + 1), GAME_WIDTH / 2, afterQuestionY + 17, new Color(255, 150, 100));
        }
        if (!whyLines.isEmpty()) {
            g2.setFont(whyFont);
            int wy = afterQuestionY + missLineH + 6 + wfm.getAscent();
            for (int i = 0; i < whyLines.size(); i++) {
                game.drawCenteredShadowed(g2, whyLines.get(i), GAME_WIDTH / 2, wy + i * wfm.getHeight(), new Color(170, 215, 255));
            }
        }

        int barX = GAME_WIDTH / 2 - barW / 2;
        int barY = afterQuestionY + missLineH + whyH + barGap - 4;
        double ratio = clamp(game.timeLeft / game.maxTime, 0, 1);
        boolean panic = game.timeLeft <= 5.0 && !game.timeExpired;

        // In the last five seconds the bar swells and glows in time with the tick.
        if (panic) {
            double beat = game.timeLeft - Math.floor(game.timeLeft);
            float surge = (float) Math.pow(beat, 2.2);
            int grow = (int) (surge * 5);
            g2.setColor(new Color(255, 60, 60, (int) (40 + surge * 130)));
            g2.fillRoundRect(barX - grow - 4, barY - grow - 3, barW + grow * 2 + 8, barH + grow * 2 + 6, 12, 12);
        }

        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
        g2.setColor(ratio > 0.5 ? new Color(80, 220, 100) : ratio > 0.25 ? Color.ORANGE : Color.RED);
        g2.fillRoundRect(barX, barY, (int) (barW * ratio), barH, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(barX, barY, barW, barH, 8, 8);

        int secs = (int) Math.ceil(game.timeLeft);
        g2.setFont(font(Font.BOLD, 14));
        g2.setColor(ratio > 0.5 ? new Color(80, 220, 100) : ratio > 0.25 ? Color.ORANGE : Color.RED);
        g2.drawString(secs + "s", barX + barW + 10, barY + barH - 1);

        if (game.timeExpired && !game.bullets.isEmpty()) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2.2));
            g2.setColor(new Color(255, 230, 60, (int) (180 + pulse * 75)));
            g2.setFont(font(Font.BOLD, 16));
            game.drawCentered(g2, "BUZZER BEATER!", GAME_WIDTH / 2, barY + barH + 22);
        }
    }

    void drawBanner(Graphics2D g2) {
        double elapsed = game.bannerDuration - game.bannerTimer;
        double punchT = clamp(elapsed / 0.22, 0, 1);
        double pop = game.bannerEmphasis ? 1.0 + 0.55 * (1 - punchT) * (1 - punchT) : 1.0 + 0.2 * (1 - punchT);
        int barH = game.bannerEmphasis ? 96 : 80;
        double maxPop = game.bannerEmphasis ? 1.55 : 1.2;
        int fontSize = game.bannerEmphasis ? 48 : 40;
        while (fontSize > 20) {
            if (g2.getFontMetrics(font(Font.BOLD, fontSize)).stringWidth(game.bannerText) * maxPop <= GAME_WIDTH - 48) break;
            fontSize -= 2;
        }

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, GAME_HEIGHT / 2 - barH / 2, GAME_WIDTH, barH);

        if (game.bannerEmphasis && game.graphics.glows) {
            float glowPulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2));
            Paint old = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0, GAME_HEIGHT / 2.0 - 5), 260f,
                new float[]{0f, 1f},
                new Color[]{
                    new Color(game.bannerColor.getRed(), game.bannerColor.getGreen(), game.bannerColor.getBlue(), (int) (90 + glowPulse * 60)),
                    new Color(game.bannerColor.getRed(), game.bannerColor.getGreen(), game.bannerColor.getBlue(), 0)
                }));
            g2.fillRect(0, GAME_HEIGHT / 2 - barH / 2, GAME_WIDTH, barH);
            g2.setPaint(old);
        }

        AffineTransform oldTx = g2.getTransform();
        g2.translate(GAME_WIDTH / 2.0, GAME_HEIGHT / 2.0 - 5);
        g2.scale(pop, pop);
        g2.setFont(font(Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(game.bannerText);
        if (game.bannerEmphasis) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(game.bannerText, -w / 2 + 2, 2);
        }
        g2.setColor(game.bannerColor);
        g2.drawString(game.bannerText, -w / 2, 0);
        g2.setTransform(oldTx);

        if (game.bannerSubtitle != null && !game.bannerSubtitle.isEmpty()) {
            g2.setFont(font(Font.PLAIN, game.textSize(17)));
            FontMetrics sfm = g2.getFontMetrics();
            List<String> lines = new ArrayList<>();
            for (String part : game.bannerSubtitle.split("\n")) lines.addAll(game.wrapText(sfm, part, 700));
            int lineH = sfm.getHeight();
            int boxH = lines.size() * lineH + 18;
            int boxY = GAME_HEIGHT / 2 + barH / 2 + 8;
            int boxW = 0;
            for (String ln : lines) boxW = Math.max(boxW, sfm.stringWidth(ln));
            boxW += 36;

            g2.setColor(game.highContrast ? Color.BLACK : new Color(8, 10, 16, 190));
            g2.fillRoundRect(GAME_WIDTH / 2 - boxW / 2, boxY, boxW, boxH, 14, 14);
            g2.setColor(new Color(game.bannerColor.getRed(), game.bannerColor.getGreen(), game.bannerColor.getBlue(), 140));
            g2.drawRoundRect(GAME_WIDTH / 2 - boxW / 2, boxY, boxW, boxH, 14, 14);

            int ty = boxY + 9 + sfm.getAscent();
            for (int i = 0; i < lines.size(); i++) {
                g2.setColor(new Color(225, 232, 242));
                game.drawCentered(g2, lines.get(i), GAME_WIDTH / 2, ty + i * lineH);
            }

            if (game.bannerAwaitInput) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2.4));
                g2.setFont(font(Font.BOLD, 15));
                g2.setColor(new Color(255, 225, 90, (int) (150 + pulse * 105)));
                game.drawCentered(g2, "Press " + KeyEvent.getKeyText(game.keyShoot) + " or click when you're ready to continue",
                    GAME_WIDTH / 2, boxY + boxH + 26);
            }
        }
    }
}
