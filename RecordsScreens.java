package rocketlearn;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import static rocketlearn.RocketLearn.*;

/**
 * High scores, lifetime accuracy and the initials screen. Where the name
 * boxes sit is shared with the mouse, so that geometry stays in RocketLearn.
 */
class RecordsScreens {

    final RocketLearn game;

    RecordsScreens(RocketLearn game) { this.game = game; }

    void drawHighScoresScreen(Graphics2D g2) {
        g2.setColor(new Color(6, 8, 18, 230));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 30));
        game.drawCentered(g2, "RECORDS", GAME_WIDTH / 2, 50);

        drawLifetimeStats(g2);

        if (game.highScores.isEmpty()) {
            g2.setColor(Color.WHITE);
            g2.setFont(font(Font.PLAIN, 18));
            game.drawCentered(g2, "No runs recorded yet - go set one.", GAME_WIDTH / 2, 300);
        } else {
            int rowH = 38;
            int tableW = 520, tableX = GAME_WIDTH / 2 - tableW / 2;
            int y = 232;

            g2.setFont(font(Font.BOLD, 13));
            g2.setColor(new Color(140, 155, 180));
            g2.drawString("RANK", tableX + 18, y);
            g2.drawString("NAME", tableX + 110, y);
            g2.drawString("SCORE", tableX + 250, y);
            g2.drawString("LEVEL", tableX + 400, y);
            y += 14;

            List<HighScores.Entry> all = game.highScores.all();
            for (int i = 0; i < all.size(); i++) {
                HighScores.Entry e = all.get(i);
                boolean fresh = i == game.newHighScoreRank;
                int rowY = y + i * rowH;

                g2.setColor(fresh ? new Color(60, 52, 12, 220) : new Color(14, 18, 30, 190));
                g2.fillRoundRect(tableX, rowY, tableW, rowH - 6, 12, 12);
                if (fresh) {
                    float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2));
                    g2.setColor(new Color(255, 215, 0, (int) (140 + pulse * 110)));
                    g2.drawRoundRect(tableX, rowY, tableW, rowH - 6, 12, 12);
                }

                g2.setColor(i == 0 ? new Color(255, 215, 0) : Color.WHITE);
                g2.setFont(font(Font.BOLD, 21));
                g2.drawString("#" + (i + 1), tableX + 18, rowY + 27);
                g2.drawString(e.name, tableX + 110, rowY + 27);
                g2.drawString(String.valueOf(e.score), tableX + 250, rowY + 27);
                g2.setFont(font(Font.PLAIN, 18));
                g2.setColor(new Color(190, 200, 215));
                g2.drawString(String.valueOf(e.level), tableX + 400, rowY + 27);
            }
        }

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 14));
        game.drawCentered(g2, "ENTER, ESC or click to go back", GAME_WIDTH / 2, GAME_HEIGHT - 26);
    }

    /** Lifetime accuracy and the weakest subject, accumulated across every run. */
    void drawLifetimeStats(Graphics2D g2) {
        int panelW = 640, panelX = GAME_WIDTH / 2 - panelW / 2, panelY = 70, panelH = 138;
        g2.setColor(new Color(10, 14, 24, 200));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 14, 14);
        g2.setColor(new Color(90, 150, 210, 110));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 14, 14);

        g2.setFont(font(Font.BOLD, 12));
        g2.setColor(new Color(140, 160, 190));
        g2.drawString("ALL TIME", panelX + 20, panelY + 22);

        if (game.stats.isEmpty()) {
            g2.setFont(font(Font.PLAIN, 16));
            g2.setColor(new Color(170, 180, 200));
            game.drawCentered(g2, "Answer some questions and your accuracy will show up here.",
                GAME_WIDTH / 2, panelY + 70);
            return;
        }

        int pct = game.stats.accuracyPercent();
        g2.setFont(font(Font.BOLD, 34));
        g2.setColor(pct >= 80 ? new Color(90, 225, 120) : pct >= 60 ? Color.ORANGE : new Color(235, 110, 100));
        g2.drawString(pct + "%", panelX + 20, panelY + 60);

        g2.setFont(font(Font.PLAIN, 13));
        g2.setColor(new Color(180, 190, 210));
        g2.drawString(game.stats.correct + " correct of " + game.stats.answered + " answered", panelX + 20, panelY + 80);

        String weak = game.stats.weakestTopic();
        if (weak != null) {
            g2.setFont(font(Font.BOLD, 13));
            g2.setColor(new Color(235, 130, 110));
            g2.drawString("WEAKEST TOPIC", panelX + 230, panelY + 22);
            g2.setFont(font(Font.BOLD, 17));
            g2.setColor(Color.WHITE);
            g2.drawString(weak + "  -  " + game.stats.topicAccuracy(weak) + "%", panelX + 230, panelY + 46);
        }

        // A short bar per topic, worst first.
        List<Map.Entry<String, int[]>> rows = new ArrayList<>(game.stats.byTopic.entrySet());
        rows.sort((a, b) -> Integer.compare(
            a.getValue()[0] * 100 / Math.max(1, a.getValue()[1]),
            b.getValue()[0] * 100 / Math.max(1, b.getValue()[1])));

        int barX = panelX + 230, barW = 260;
        int rowY = panelY + 64;
        for (int i = 0; i < Math.min(3, rows.size()); i++) {
            Map.Entry<String, int[]> e = rows.get(i);
            int p = e.getValue()[0] * 100 / Math.max(1, e.getValue()[1]);
            g2.setFont(font(Font.PLAIN, 11));
            g2.setColor(new Color(175, 185, 205));
            g2.drawString(e.getKey(), barX, rowY + 8);
            g2.setColor(new Color(40, 46, 60));
            g2.fillRoundRect(barX + 130, rowY, barW - 130, 8, 4, 4);
            g2.setColor(p >= 80 ? new Color(90, 225, 120) : p >= 60 ? Color.ORANGE : new Color(235, 110, 100));
            g2.fillRoundRect(barX + 130, rowY, (int) ((barW - 130) * (p / 100.0)), 8, 4, 4);
            g2.setColor(new Color(200, 210, 225));
            g2.drawString(p + "%", barX + barW + 8, rowY + 8);
            rowY += 20;
        }
    }

    void drawNameEntryScreen(Graphics2D g2) {
        g2.setColor(new Color(4, 6, 16, 225));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.2));
        if (game.graphics.glows) {
            Paint oldPaint = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0, GAME_HEIGHT / 2.0 - 40), 300f,
                new float[]{0f, 1f},
                new Color[]{ new Color(255, 215, 0, (int) (55 + pulse * 35)), new Color(255, 215, 0, 0) }));
            g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            g2.setPaint(oldPaint);
        }

        g2.setColor(new Color(255, 215, 0));
        g2.setFont(font(Font.BOLD, 40));
        game.drawCentered(g2, "NEW HIGH SCORE!", GAME_WIDTH / 2, GAME_HEIGHT / 2 - 110);

        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.PLAIN, 21));
        game.drawCentered(g2, "Score " + game.score + "   " + '•' + "   Level " + game.level, GAME_WIDTH / 2, GAME_HEIGHT / 2 - 74);

        int y = NAME_SLOT_TOP, slotW = NAME_SLOT_W, slotH = NAME_SLOT_H;
        int[] hover = nameEntryHit(game.mouseGX, game.mouseGY);

        for (int i = 0; i < HighScores.NAME_LENGTH; i++) {
            int sx = nameSlotX(i);
            boolean active = i == game.nameEntryIndex;
            boolean hovered = hover[0] == i;

            g2.setColor(new Color(12, 16, 28, 210));
            g2.fillRoundRect(sx, y, slotW, slotH, 12, 12);
            g2.setColor(active ? new Color(255, 215, 0, (int) (170 + pulse * 85))
                : hovered && hover[1] == 0 ? new Color(170, 180, 205) : new Color(90, 100, 125));
            g2.setStroke(new BasicStroke(active ? 3f : 1.5f));
            g2.drawRoundRect(sx, y, slotW, slotH, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            g2.setColor(active ? Color.YELLOW : Color.WHITE);
            g2.setFont(font(Font.BOLD, 46));
            game.drawCentered(g2, String.valueOf(game.nameEntry[i]), sx + slotW / 2, y + 57);

            // Every box has its arrows so the mouse can change any letter;
            // the active box's are gold, a hovered arrow lights up.
            g2.setFont(font(Font.BOLD, 16));
            Color arrowRest = active ? new Color(255, 215, 0, 190) : new Color(110, 118, 140);
            g2.setColor(hovered && hover[1] == 1 ? Color.WHITE : arrowRest);
            game.drawCentered(g2, "▲", sx + slotW / 2, y - 8);
            g2.setColor(hovered && hover[1] == -1 ? Color.WHITE : arrowRest);
            game.drawCentered(g2, "▼", sx + slotW / 2, y + slotH + 20);
        }

        // SAVE, for finishing with the mouse alone.
        boolean saveHover = hover[0] == NAME_HIT_SAVE;
        int bx = GAME_WIDTH / 2 - NAME_SAVE_W / 2, by = NAME_SAVE_TOP;
        g2.setColor(saveHover ? new Color(255, 215, 0) : new Color(255, 215, 0, 40));
        g2.fillRoundRect(bx, by, NAME_SAVE_W, NAME_SAVE_H, NAME_SAVE_H, NAME_SAVE_H);
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bx, by, NAME_SAVE_W, NAME_SAVE_H, NAME_SAVE_H, NAME_SAVE_H);
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(font(Font.BOLD, 17));
        g2.setColor(saveHover ? new Color(40, 28, 0) : new Color(255, 225, 120));
        game.drawCentered(g2, "SAVE", GAME_WIDTH / 2, by + 22);

        g2.setColor(new Color(190, 200, 215));
        g2.setFont(font(Font.PLAIN, 15));
        game.drawCentered(g2, "Up/Down, scroll or click " + '▲' + '▼' + " to change a letter  " + '•'
            + "  Left/Right or click a box to move  " + '•' + "  type A-Z directly",
            GAME_WIDTH / 2, GAME_HEIGHT / 2 + 128);
        game.drawCentered(g2, "ENTER or click SAVE to save", GAME_WIDTH / 2, GAME_HEIGHT / 2 + 152);
    }
}
