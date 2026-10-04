package rocketlearn;

import java.awt.*;
import java.util.*;
import java.util.List;

import static rocketlearn.RocketLearn.*;

/**
 * The Practice hub and what it opens: topic stats, the notebook and the
 * end-of-run review of mistakes.
 */
class StudyScreens {

    final RocketLearn game;

    StudyScreens(RocketLearn game) { this.game = game; }

    void drawReviewScreen(Graphics2D g2) {
        game.reviewIndex = drawQuestionBrowser(g2, "REVIEW YOUR MISTAKES", game.missedQuestions, game.reviewIndex,
            "No mistakes to review - flawless run!", false);
    }

    void drawNotebookScreen(Graphics2D g2) {
        game.notebookIndex = drawQuestionBrowser(g2, "NOTEBOOK", game.notebook.entries, game.notebookIndex,
            "Nothing here yet - questions you miss are kept here to reread.", true);
    }

    /**
     * One question per page with its answer and explanation, browsed left and
     * right. Returns the index actually shown, clamped to the list.
     */
    int drawQuestionBrowser(Graphics2D g2, String title, List<Question> list, int index, String emptyText, boolean showCleared) {
        g2.setColor(new Color(6, 8, 18, 235));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 30));
        game.drawCentered(g2, title, GAME_WIDTH / 2, 48);

        if (list.isEmpty()) {
            g2.setColor(Color.WHITE);
            g2.setFont(font(Font.PLAIN, 20));
            game.drawCentered(g2, emptyText, GAME_WIDTH / 2, GAME_HEIGHT / 2);
            g2.setColor(new Color(180, 180, 180));
            g2.setFont(font(Font.PLAIN, 14));
            game.drawCentered(g2, "ESC, click or right-click to go back", GAME_WIDTH / 2, GAME_HEIGHT - 28);
            return 0;
        }

        index = clampInt(index, 0, list.size() - 1);
        Question q = list.get(index);

        g2.setColor(new Color(170, 180, 200));
        g2.setFont(font(Font.BOLD, 14));
        game.drawCentered(g2, (index + 1) + " of " + list.size(), GAME_WIDTH / 2, 74);

        String tag = q.topic;
        boolean cleared = showCleared && game.notebook.isCleared(q);
        if (showCleared) tag += cleared ? "   ✓ cleared"
            : game.notebook.isRecovered(q) ? "   ½ got it on a retry - to clear" : "   to clear";
        g2.setFont(font(Font.BOLD, 13));
        FontMetrics tagFm = g2.getFontMetrics();
        int tagW = tagFm.stringWidth(tag) + 24;
        boolean recovered = showCleared && !cleared && game.notebook.isRecovered(q);
        g2.setColor(cleared ? new Color(30, 80, 45, 220) : recovered ? new Color(85, 65, 20, 220) : new Color(40, 70, 110, 220));
        g2.fillRoundRect(GAME_WIDTH / 2 - tagW / 2, 84, tagW, 22, 11, 11);
        g2.setColor(cleared ? new Color(120, 230, 150) : recovered ? new Color(255, 200, 100) : new Color(130, 200, 255));
        g2.drawRoundRect(GAME_WIDTH / 2 - tagW / 2, 84, tagW, 22, 11, 11);
        game.drawCentered(g2, tag, GAME_WIDTH / 2, 100);

        int panelX = 70, panelW = GAME_WIDTH - 140;
        int panelY = 116, panelH = GAME_HEIGHT - 116 - 62;
        g2.setColor(new Color(10, 14, 24, 220));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g2.setColor(new Color(110, 190, 255, 190));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g2.setStroke(new BasicStroke(1f));

        int y = panelY + 34;
        g2.setFont(font(Font.BOLD, 19));
        FontMetrics qfm = g2.getFontMetrics();
        List<String> qLines = game.wrapText(qfm, q.prompt, panelW - 60);
        g2.setColor(Color.YELLOW);
        for (String ln : qLines) {
            game.drawCentered(g2, ln, GAME_WIDTH / 2, y);
            y += qfm.getHeight();
        }

        if (q.hasCode()) {
            y += 10;
            y += game.drawCodeBlock(g2, q.codeLines(), y - qfm.getAscent(), panelW - 80) - qfm.getAscent() + 8;
        }

        y += 18;
        g2.setFont(font(Font.BOLD, 18));
        g2.setColor(new Color(80, 220, 100));
        game.drawCentered(g2, "Correct answer:  " + q.choices[q.correct], GAME_WIDTH / 2, y);

        y += 30;
        g2.setFont(font(Font.PLAIN, 16));
        FontMetrics wfm = g2.getFontMetrics();
        g2.setColor(new Color(215, 225, 240));
        for (String ln : game.wrapText(wfm, q.why, panelW - 70)) {
            game.drawCentered(g2, ln, GAME_WIDTH / 2, y);
            y += wfm.getHeight();
        }

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 14));
        game.drawCentered(g2, "Left/Right, click or scroll to browse  •  ESC or right-click to go back", GAME_WIDTH / 2, GAME_HEIGHT - 28);
        return index;
    }

    /** Where the practice hub's list starts and how far apart its rows sit: eight rows clear of the hint below. */
    static final int STUDY_LIST_TOP = 150, STUDY_LIST_GAP = 36;

    void drawStudyScreen(Graphics2D g2) {
        g2.setColor(new Color(6, 8, 18, 225));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 36));
        game.drawCentered(g2, "PRACTICE", GAME_WIDTH / 2, 70);
        g2.setColor(new Color(170, 185, 210));
        g2.setFont(font(Font.PLAIN, 16));
        game.drawCentered(g2, "No hearts, no score - just learning.", GAME_WIDTH / 2, 100);

        String[] items = game.studyItems();
        game.studyIndex = clampInt(game.studyIndex, 0, items.length - 1);
        game.drawSelectableList(g2, items, game.studyIndex, STUDY_LIST_TOP, STUDY_LIST_GAP, 24, 19);

        // A codex card not yet read marks the Codex row NEW.
        int codexAt = Arrays.asList(game.studyActions()).indexOf("codex");
        if (game.rewards.anyCodexUnseen() && codexAt >= 0 && codexAt != game.studyIndex) {
            int w = g2.getFontMetrics(font(Font.BOLD, 19)).stringWidth(items[codexAt]);
            game.drawNewBadge(g2, GAME_WIDTH / 2 + w / 2 + 12, STUDY_LIST_TOP + codexAt * STUDY_LIST_GAP - 15);
        }

        g2.setColor(new Color(170, 180, 200, 200));
        g2.setFont(font(Font.PLAIN, 14));
        game.drawCentered(g2, game.studyHint(game.studyIndex), GAME_WIDTH / 2, GAME_HEIGHT - 50);
        g2.setColor(new Color(180, 180, 180));
        game.drawCentered(g2, "W/S or Up/Down to choose  •  ENTER or click to select  •  ESC or right-click to go back", GAME_WIDTH / 2, GAME_HEIGHT - 22);
    }

    /**
     * The planet codex: a strip of the eight planets along the route, and the
     * card for the one selected. A finished planet's card holds its facts, the
     * topics it asks, the player's medal and accuracy there; an unfinished
     * one says what will unlock it.
     */
    void drawCodexScreen(Graphics2D g2) {
        g2.setColor(new Color(6, 8, 18, 235));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 30));
        game.drawCentered(g2, "PLANET CODEX", GAME_WIDTH / 2, 44);

        int n = TRAVEL_ORDER.length;
        game.codexIndex = ((game.codexIndex % n) + n) % n;
        boolean[] finished = game.rewards.finishedPlanets();

        // The route strip.
        int stripY = 84, gap = 64, x0 = GAME_WIDTH / 2 - (n - 1) * gap / 2;
        g2.setColor(new Color(70, 80, 110));
        g2.drawLine(x0, stripY, x0 + (n - 1) * gap, stripY);
        for (int slot = 0; slot < n; slot++) {
            int p = TRAVEL_ORDER[slot];
            Planet planet = planets.get(p);
            int cx = x0 + slot * gap, r = slot == game.codexIndex ? 15 : 10;
            if (slot == game.codexIndex) {
                g2.setColor(new Color(255, 215, 0, 190));
                g2.fill(new java.awt.geom.Ellipse2D.Double(cx - r - 4, stripY - r - 4, (r + 4) * 2, (r + 4) * 2));
            }
            g2.setColor(finished[p] ? planet.base : new Color(45, 48, 60));
            g2.fill(new java.awt.geom.Ellipse2D.Double(cx - r, stripY - r, r * 2, r * 2));
            if (!finished[p]) {
                g2.setColor(new Color(130, 135, 150));
                g2.setFont(font(Font.BOLD, r + 2));
                game.drawCentered(g2, "?", cx, stripY + (r + 2) / 2 - 1);
            }
            if (game.rewards.codexUnseen(p) && slot != game.codexIndex) game.drawNewBadge(g2, cx - 14, stripY - 32);
        }

        int planetIdx = TRAVEL_ORDER[game.codexIndex];
        Planet planet = planets.get(planetIdx);
        int panelX = 50, panelY = 114, panelW = GAME_WIDTH - 100, panelH = GAME_HEIGHT - panelY - 50;
        g2.setColor(new Color(10, 14, 24, 225));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g2.setColor(finished[planetIdx] ? lerpColor(planet.base, Color.WHITE, 0.3f) : new Color(80, 86, 104));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g2.setStroke(new BasicStroke(1f));

        int artX = panelX + 120, artY = panelY + panelH / 2, artR = 62;
        if (!finished[planetIdx]) {
            g2.setColor(new Color(40, 44, 58));
            g2.fill(new java.awt.geom.Ellipse2D.Double(artX - artR, artY - artR, artR * 2, artR * 2));
            g2.setColor(new Color(120, 126, 145));
            g2.setFont(font(Font.BOLD, 54));
            game.drawCentered(g2, "?", artX, artY + 19);
            g2.setColor(Color.WHITE);
            g2.setFont(font(Font.BOLD, 26));
            g2.drawString(planet.name, panelX + 230, panelY + 70);
            g2.setColor(new Color(170, 180, 200));
            g2.setFont(font(Font.PLAIN, 16));
            g2.drawString("Finish " + planet.name + " to unlock this card.", panelX + 230, panelY + 104);
            g2.drawString("It brings its ship paint, engine trail and hull badge too.", panelX + 230, panelY + 128);
        } else {
            if (planet.ringed) {
                g2.setColor(planet.accent);
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new java.awt.geom.Ellipse2D.Double(artX - artR * 1.5, artY - artR * 0.35, artR * 3, artR * 0.7));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.setColor(planet.base);
            g2.fill(new java.awt.geom.Ellipse2D.Double(artX - artR, artY - artR, artR * 2, artR * 2));
            game.scenery.drawPlanetFeatures(g2, planetIdx, artX, artY, artR, planet.base, planet.accent);
            PlanetRecords.Medal medal = game.planetRecords.shown(planetIdx, game.highestLevelReached);
            MenuScreens.drawMedal(g2, medal, artX + artR - 6, artY + artR - 6, 13);

            int tx = panelX + 230, textW = panelX + panelW - 30 - tx, y = panelY + 46;
            g2.setColor(Color.WHITE);
            g2.setFont(font(Font.BOLD, 26));
            g2.drawString(planet.name, tx, y);
            g2.setFont(font(Font.BOLD, 14));
            g2.setColor(medal.color);
            g2.drawString(medal.label + " medal", tx + g2.getFontMetrics(font(Font.BOLD, 26)).stringWidth(planet.name) + 14, y);
            y += 24;
            g2.setColor(lerpColor(planet.base, Color.WHITE, 0.45f));
            g2.setFont(font(Font.BOLD, 14));
            g2.drawString(planet.traitName + " - " + planet.traitDesc, tx, y);

            y += 28;
            g2.setFont(font(Font.PLAIN, 15));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(new Color(215, 225, 240));
            List<String> facts = new ArrayList<>();
            facts.add(planet.fact);
            facts.addAll(Arrays.asList(PlanetRecords.CODEX_FACTS[planetIdx]));
            for (String fact : facts) {
                List<String> lines = game.wrapText(fm, fact, textW - 16);
                for (int i = 0; i < lines.size(); i++) {
                    g2.drawString((i == 0 ? "•  " : "    ") + lines.get(i), tx, y);
                    y += fm.getHeight();
                }
                y += 4;
            }

            y += 10;
            g2.setFont(font(Font.BOLD, 13));
            g2.setColor(new Color(140, 200, 235));
            for (String ln : game.wrapText(g2.getFontMetrics(), "Topics asked: " + String.join(", ", game.destinationTopics(planetIdx)), textW)) {
                g2.drawString(ln, tx, y);
                y += 17;
            }
            g2.setColor(new Color(170, 185, 210));
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString(game.destinationMasteryLine(planetIdx), tx, y + 4);
        }

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 14));
        game.drawCentered(g2, "Left/Right, click or scroll to browse  •  ESC or right-click to go back", GAME_WIDTH / 2, GAME_HEIGHT - 20);
    }

    void drawTopicStatsScreen(Graphics2D g2) {
        g2.setColor(new Color(6, 8, 18, 230));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 30));
        game.drawCentered(g2, "TOPIC STATS", GAME_WIDTH / 2, 46);
        g2.setColor(new Color(170, 185, 210));
        g2.setFont(font(Font.PLAIN, 15));
        game.drawCentered(g2, game.stats.isEmpty() ? "No answers yet - practice any topic to start filling this in."
            : "All time:  " + game.stats.accuracyPercent() + "%  (" + game.stats.correct + " of " + game.stats.answered + " right first try)",
            GAME_WIDTH / 2, 74);

        int rows = game.topicStatsRows();
        game.topicStatsIndex = clampInt(game.topicStatsIndex, 0, rows - 1);

        // Hit-testing reuses the list geometry the menus register.
        game.uiListState = game.state;
        game.uiListStartY = TOPIC_ROWS_TOP + 4;   // listIndexAt centres a row 10px above this
        game.uiListGap = TOPIC_ROW_GAP;
        game.uiListCount = rows;

        int tableW = 700, tableX = GAME_WIDTH / 2 - tableW / 2;
        g2.setFont(font(Font.BOLD, 11));
        g2.setColor(new Color(140, 155, 180));
        int headerY = TOPIC_ROWS_TOP - 26;   // clear of the first row, which starts 20px above TOPIC_ROWS_TOP
        g2.drawString("TOPIC", tableX + 16, headerY);
        g2.drawString("ACCURACY", tableX + 250, headerY);
        g2.drawString("RIGHT / ASKED", tableX + 500, headerY);
        g2.drawString("TO CLEAR", tableX + 610, headerY);

        Map<String, Integer> toClearByTopic = new HashMap<>();
        for (Question q : game.notebook.entries) if (!game.notebook.isCleared(q)) toClearByTopic.merge(q.topic, 1, Integer::sum);
        String weakest = game.stats.weakestTopic();

        for (int i = 0; i < rows; i++) {
            int y = TOPIC_ROWS_TOP + i * TOPIC_ROW_GAP - 20;
            boolean sel = i == game.topicStatsIndex;
            g2.setColor(sel ? new Color(60, 52, 12, 220) : new Color(14, 18, 30, 190));
            g2.fillRoundRect(tableX, y, tableW, TOPIC_ROW_GAP - 5, 10, 10);
            if (sel) {
                g2.setColor(new Color(255, 215, 0, 200));
                g2.drawRoundRect(tableX, y, tableW, TOPIC_ROW_GAP - 5, 10, 10);
            }
            int textY = y + 19;

            if (i == rows - 1) {
                g2.setFont(font(Font.BOLD, 15));
                g2.setColor(sel ? Color.YELLOW : Color.WHITE);
                game.drawCentered(g2, "Back", GAME_WIDTH / 2, textY);
                continue;
            }

            String topic = ALL_TOPICS.get(i);
            int[] t = game.stats.byTopic.get(topic);
            g2.setFont(font(Font.BOLD, 14));
            g2.setColor(sel ? Color.YELLOW : Color.WHITE);
            g2.drawString(topic + (topic.equals(weakest) ? "  (weakest)" : ""), tableX + 16, textY);

            int barX = tableX + 250, barW = 180;
            g2.setColor(new Color(40, 46, 60));
            g2.fillRoundRect(barX, textY - 9, barW, 10, 5, 5);
            g2.setFont(font(Font.PLAIN, 13));
            if (t == null || t[1] == 0) {
                g2.setColor(new Color(120, 130, 150));
                g2.drawString("not tried yet", barX + barW + 10, textY);
            } else {
                int p = (int) Math.round(t[0] * 100.0 / t[1]);
                g2.setColor(p >= 80 ? new Color(90, 225, 120) : p >= 60 ? Color.ORANGE : new Color(235, 110, 100));
                g2.fillRoundRect(barX, textY - 9, (int) (barW * p / 100.0), 10, 5, 5);
                g2.setColor(new Color(215, 222, 235));
                g2.drawString(p + "%", barX + barW + 10, textY);
                g2.drawString(t[0] + " / " + t[1], tableX + 500, textY);
            }
            int clear = toClearByTopic.getOrDefault(topic, 0);
            g2.setColor(clear > 0 ? new Color(255, 170, 120) : new Color(110, 120, 140));
            g2.drawString(clear > 0 ? String.valueOf(clear) : "-", tableX + 630, textY);
        }

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 14));
        game.drawCentered(g2, "ENTER or click a topic to practice it  •  ESC or right-click to go back", GAME_WIDTH / 2, GAME_HEIGHT - 16);
    }
}
