package rocketlearn;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.*;

import static rocketlearn.RocketLearn.*;

/**
 * The menu-style screens around a run: the main menu, level select, pause,
 * game over and the how-to-play guide. Drawing only - what a choice does is
 * InputHandler's, and the state behind it is RocketLearn's.
 */
class MenuScreens {

    final RocketLearn game;

    MenuScreens(RocketLearn game) { this.game = game; }

    static final String[] HOW_TO_TITLES = { "FLYING", "SCORING", "POWER-UPS & PLANETS" };

    void drawMenuNebula(Graphics2D g2) {
        if (!game.graphics.liveWashes) {
            // Held still at the middle of its pulse, so it can be painted once and stamped.
            game.scenery.overlays.draw(g2, "nebula", 0, 0, GAME_WIDTH, GAME_HEIGHT, ig -> paintMenuNebula(ig, 0.5f));
            return;
        }
        Paint old = g2.getPaint();
        paintMenuNebula(g2, (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 0.3)));
        g2.setPaint(old);
    }

    /** The menu's backdrop: a violet-to-navy wash with two clouds of light that trade brightness as pulse moves. */
    void paintMenuNebula(Graphics2D g2, float pulse) {
        g2.setPaint(new GradientPaint(0, 0, new Color(35, 12, 55, 225), GAME_WIDTH, GAME_HEIGHT, new Color(10, 18, 50, 225)));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(GAME_WIDTH * 0.22, GAME_HEIGHT * 0.28), 280f,
            new float[]{0f, 1f},
            new Color[]{ new Color(170, 70, 210, (int) (55 + pulse * 25)), new Color(170, 70, 210, 0) }));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(GAME_WIDTH * 0.8, GAME_HEIGHT * 0.78), 320f,
            new float[]{0f, 1f},
            new Color[]{ new Color(60, 100, 210, (int) (50 + (1 - pulse) * 25)), new Color(60, 100, 210, 0) }));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
    }

    String menuHint(int idx) {
        String[] actions = game.menuActions();
        if (idx < 0 || idx >= actions.length) return "";
        return switch (actions[idx]) {
            case "continue" -> "Pick up where you left off on " + planets.get(planetIndexForLevel(game.lastLevel)).name;
            case "school" -> "A short guided first flight: steer, shoot, answer, boost - nothing to lose";
            case "start" -> "Begin a fresh run from Level 1";
            case "practice" -> "Drills, your mistakes, a notebook of explanations, and topic stats";
            case "select" -> "Jump to any planet you've already reached";
            case "howto" -> "Controls, scoring, power-ups and planet traits";
            case "records" -> "High scores and your lifetime accuracy";
            case "settings" -> "Adjust lives, timer, sound, graphics, ship, and keybinds";
            default -> "Exit the game";
        };
    }

    void drawMenuStats(Graphics2D g2) {
        String[] labels = { "BEST SCORE", "PLANETS", "HIGHEST LEVEL" };
        String[] values = { String.valueOf(game.highScore), game.planetsUnlocked() + "/" + planets.size(), String.valueOf(game.highestLevelReached) };

        int boxW = 180, boxH = 42, gap = 12;
        int totalW = boxW * labels.length + gap * (labels.length - 1);
        int x0 = GAME_WIDTH / 2 - totalW / 2;
        int y = MENU_LIST_BOTTOM + 42;

        for (int i = 0; i < labels.length; i++) {
            int bx = x0 + i * (boxW + gap);
            // Nearly opaque, so the menu rocket reads as passing behind the boxes.
            g2.setColor(new Color(12, 16, 28, 230));
            g2.fillRoundRect(bx, y, boxW, boxH, 12, 12);
            g2.setColor(new Color(90, 150, 210, 110));
            g2.drawRoundRect(bx, y, boxW, boxH, 12, 12);

            g2.setColor(new Color(140, 160, 190, 210));
            g2.setFont(font(Font.PLAIN, 10));
            game.drawCentered(g2, labels[i], bx + boxW / 2, y + 16);

            g2.setColor(Color.WHITE);
            g2.setFont(font(Font.BOLD, 19));
            game.drawCentered(g2, values[i], bx + boxW / 2, y + 35);
        }
    }

    void drawMenuScreen(Graphics2D g2) {
        drawMenuNebula(g2);

        double t = game.flameFlicker / 20.0;
        Paint oldAsteroidPaint = g2.getPaint();
        for (double[] a : game.menuAsteroids) {
            double ax = menuAsteroidX(a, t);
            double ay = menuAsteroidY(a, t);
            double rad = a[2];
            if (!game.graphics.glows) {
                g2.setColor(new Color(150, 130, 112, 40));
                g2.fill(new Ellipse2D.Double(ax - rad * 0.7, ay - rad * 0.7, rad * 1.4, rad * 1.4));
                continue;
            }
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(ax, ay), (float) rad,
                new float[]{0f, 1f},
                new Color[]{ new Color(150, 130, 112, 90), new Color(150, 130, 112, 0) }));
            g2.fill(new Ellipse2D.Double(ax - rad, ay - rad, rad * 2, rad * 2));
        }
        g2.setPaint(oldAsteroidPaint);

        game.playfield.drawParticles(g2);
        game.playfield.drawBullets(g2);

        if (game.rocketAlive) {
            AffineTransform heroTx = g2.getTransform();
            double bob = Math.sin(game.flameFlicker * 0.4) * 5 * (1 - Math.abs(game.throttle));
            g2.translate(game.menuRocketX, game.menuRocketY + bob);
            g2.scale(MENU_ROCKET_SCALE, MENU_ROCKET_SCALE);
            double savedRocketX = game.rocketX, savedRocketY = game.rocketY, savedRocketVX = game.rocketVX;
            game.rocketX = 0;
            game.rocketY = 0;
            game.rocketVX = game.menuRocketVX / MENU_SPEED_SCALE;
            game.ship.drawRocket(g2);
            game.rocketX = savedRocketX;
            game.rocketY = savedRocketY;
            game.rocketVX = savedRocketVX;
            g2.setTransform(heroTx);
        }

        if (game.graphics.glows) {
            float titleGlow = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 0.8));
            Paint oldPaint = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0, MENU_TITLE_Y), 220f,
                new float[]{0f, 1f},
                new Color[]{ new Color(0, 220, 255, (int) (45 + titleGlow * 35)), new Color(0, 220, 255, 0) }));
            g2.fillRect(0, MENU_TITLE_Y - 100, GAME_WIDTH, 200);
            g2.setPaint(oldPaint);
        }

        float titlePulse = (float) (1.0 + 0.025 * Math.sin(game.flameFlicker * 0.6));
        AffineTransform titleTx = g2.getTransform();
        g2.translate(GAME_WIDTH / 2.0, MENU_TITLE_Y);
        g2.scale(titlePulse, titlePulse);
        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 56));
        game.drawCentered(g2, "ROCKET LEARN", 0, 0);
        g2.setTransform(titleTx);

        g2.setColor(new Color(0, 220, 255, 130));
        g2.fillRoundRect(GAME_WIDTH / 2 - 90, MENU_TITLE_Y + 25, 180, 3, 3, 3);

        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.PLAIN, 18));
        game.drawCentered(g2, "The rocket adapts. So do you.", GAME_WIDTH / 2, MENU_LIST_TOP - 34);

        String[] items = game.menuItems();
        game.menuIndex = clampInt(game.menuIndex, 0, items.length - 1);
        for (int i = 0; i < items.length; i++) items[i] = (i + 1) + ".  " + items[i];

        // Fit the list inside a fixed band rather than guessing per item count,
        // so the highlight never reaches the tagline above or the hint below.
        int n = items.length;
        int gap = n > 1 ? Math.min(MENU_MAX_GAP, (MENU_LIST_BOTTOM - MENU_LIST_TOP) / (n - 1)) : 0;
        int startY = (MENU_LIST_TOP + MENU_LIST_BOTTOM) / 2 - (n - 1) * gap / 2;
        int selSize = gap >= 42 ? 27 : gap >= 36 ? 24 : gap >= 30 ? 22 : 20;
        game.drawSelectableList(g2, items, game.menuIndex, startY, gap, selSize, selSize - 4);

        // New rewards waiting: codex cards under Practice, paints and trails under Settings.
        String[] actions = game.menuActions();
        FontMetrics itemFm = g2.getFontMetrics(font(Font.BOLD, selSize - 4));
        for (int i = 0; i < actions.length && i < items.length; i++) {
            boolean fresh = actions[i].equals("practice") && game.rewards.anyCodexUnseen()
                || actions[i].equals("settings") && (game.rewards.skinsUnseen() || game.rewards.trailsUnseen());
            if (fresh && i != game.menuIndex) {
                game.drawNewBadge(g2, GAME_WIDTH / 2 + itemFm.stringWidth(items[i]) / 2 + 12, startY + i * gap - 15);
            }
        }

        g2.setColor(new Color(170, 180, 200, 180));
        g2.setFont(font(Font.PLAIN, 13));
        game.drawCentered(g2, menuHint(game.menuIndex), GAME_WIDTH / 2, MENU_LIST_BOTTOM + 28);

        drawMenuStats(g2);

        g2.setColor(new Color(140, 150, 170, 190));
        g2.setFont(font(Font.ITALIC, 14));
        game.drawCentered(g2, "Learn fast. Aim true.", GAME_WIDTH / 2, 472);

        g2.setColor(new Color(120, 120, 120, 150));
        g2.setFont(font(Font.PLAIN, 11));
        String f11 = "v" + VERSION + "  " + '•' + "  Click or scroll to choose  " + '•' + "  F11 Fullscreen";
        g2.drawString(f11, GAME_WIDTH - 20 - g2.getFontMetrics().stringWidth(f11), GAME_HEIGHT - 12);

        game.pilots.drawMenuLabel(g2);

        // Where the questions came from, when a teacher has supplied a questions.txt.
        int noticeY = 20;
        if (QuestionFile.status != null) {
            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(QuestionFile.statusIsProblem ? new Color(255, 150, 110) : new Color(130, 210, 150));
            g2.drawString(fitNotice(g2, QuestionFile.status), 14, noticeY);
            noticeY += 16;
        }

        // Any music file that is missing or would not play, so a silent or
        // hummed track is never a mystery.
        String musicStatus = Sfx.musicStatus();
        if (musicStatus != null) {
            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(new Color(255, 190, 110));
            g2.drawString(fitNotice(g2, musicStatus), 14, noticeY);
            noticeY += 16;
        }

        // A save that could not be read, so lost progress is never a mystery either.
        if (game.saveFile.status != null) {
            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(new Color(255, 150, 110));
            g2.drawString(fitNotice(g2, game.saveFile.status), 14, noticeY);
        }

        boolean flying = game.leftInput() || game.rightInput() || game.boostInput() || game.slowInput() || Math.abs(game.menuRocketVX) > 1 || !game.bullets.isEmpty();
        g2.setColor(flying ? new Color(120, 220, 255, 190) : new Color(120, 120, 120, 150));
        g2.drawString(KeyEvent.getKeyText(game.keyLeft) + "/" + KeyEvent.getKeyText(game.keyRight) + " Fly  •  "
            + KeyEvent.getKeyText(game.keyBoost) + "/" + KeyEvent.getKeyText(game.keySlow) + " Thrusters  •  "
            + KeyEvent.getKeyText(game.keyShoot) + " Shoot", 20, GAME_HEIGHT - 12);
    }

    /** A medal: a short ribbon above a metal disc with a lit edge and a star stamped in it. */
    static void drawMedal(Graphics2D g2, PlanetRecords.Medal medal, double cx, double cy, double r) {
        Color metal = medal.color;
        g2.setColor(new Color(60, 90, 170));
        g2.fill(new Rectangle2D.Double(cx - r * 0.75, cy - r * 2.1, r * 0.6, r * 1.3));
        g2.setColor(new Color(190, 50, 60));
        g2.fill(new Rectangle2D.Double(cx + r * 0.15, cy - r * 2.1, r * 0.6, r * 1.3));
        g2.setPaint(new GradientPaint((float) (cx - r), (float) (cy - r), metal.brighter(),
            (float) (cx + r), (float) (cy + r), metal.darker()));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.setPaint(null);
        g2.setColor(metal.darker().darker());
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        // The medal's initial, so bronze, silver and gold never depend on telling colours apart.
        String letter = medal.label.substring(0, 1);
        Font old = g2.getFont();
        g2.setFont(font(Font.BOLD, Math.max(8, (int) Math.round(r * 1.25))));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(30, 24, 18, 220));
        g2.drawString(letter, (float) (cx - fm.stringWidth(letter) / 2.0), (float) (cy + fm.getAscent() * 0.36));
        g2.setFont(old);
    }

    /** A corner notice cut short with "..." so it stops before the pilot label on the right. */
    static String fitNotice(Graphics2D g2, String text) {
        FontMetrics fm = g2.getFontMetrics();
        int room = (int) PilotScreen.LABEL.getX() - 14 - 12;
        if (fm.stringWidth(text) <= room) return text;
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "...") > room) s = s.substring(0, s.length() - 1);
        return s.trim() + "...";
    }

    /** The star's slot on the level select row: a radiant burst instead of a disc. */
    void drawStarSlot(Graphics2D g2, int cx, int cy, int r, boolean sel, boolean locked) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.5));
        if (sel) {
            Color glow = locked ? new Color(170, 60, 60) : new Color(255, 215, 0);
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), (int) (120 + pulse * 120)));
            g2.fill(new Ellipse2D.Double(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2));
        }

        if (!locked) {
            Paint old = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), r * 1.9f,
                new float[]{0f, 1f},
                new Color[]{ new Color(255, 210, 110, 130), new Color(255, 190, 70, 0) }));
            g2.fill(new Ellipse2D.Double(cx - r * 1.9, cy - r * 1.9, r * 3.8, r * 3.8));
            g2.setPaint(old);
        }

        Path2D burst = new Path2D.Double();
        for (int k = 0; k < 10; k++) {
            double ang = k * Math.PI / 5 - Math.PI / 2;
            double rad = k % 2 == 0 ? r * 1.05 : r * 0.42;
            double px = cx + Math.cos(ang) * rad, py = cy + Math.sin(ang) * rad;
            if (k == 0) burst.moveTo(px, py); else burst.lineTo(px, py);
        }
        burst.closePath();
        g2.setColor(locked ? new Color(96, 92, 82) : new Color(255, 232, 150));
        g2.fill(burst);
        if (!locked) {
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fill(new Ellipse2D.Double(cx - r * 0.3, cy - r * 0.3, r * 0.6, r * 0.6));
        }

        g2.setColor(locked ? new Color(100, 100, 100) : sel ? Color.WHITE : new Color(160, 160, 160));
        g2.setFont(font(Font.BOLD, sel ? 15 : 12));
        game.drawCentered(g2, Planet.STAR.name, cx, cy + r + 20);

        g2.setFont(font(Font.BOLD, sel ? 13 : 10));
        if (locked) {
            g2.setColor(new Color(150, 90, 90));
            game.drawCentered(g2, "LOCKED", cx, cy + r + (sel ? 38 : 34));
        } else {
            g2.setColor(new Color(255, 215, 0));
            game.drawCentered(g2, "FINAL", cx, cy + r + (sel ? 38 : 34));
        }
    }

    void drawLevelSelectScreen(Graphics2D g2) {
        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 40));
        game.drawCentered(g2, "SELECT LEVEL", GAME_WIDTH / 2, 80);

        // Nine destinations: eight planets and the star at the end.
        int n = game.destinationCount();
        int slotW = GAME_WIDTH / n;
        int cy = GAME_HEIGHT / 2 - 20;
        game.uiPlanetState = game.state;
        game.uiPlanetCy = cy;
        game.uiPlanetSlotW = slotW;
        for (int i = 0; i < n; i++) {
            int dest = destinationAtSlot(i);
            int cx = slotW * i + slotW / 2;
            boolean sel = i == game.levelSelectIndex;
            boolean locked = game.isDestinationLocked(dest);
            int r = sel ? 34 : 24;

            if (dest == STAR_DEST) {
                drawStarSlot(g2, cx, cy, r, sel, locked);
                continue;
            }

            Planet p = planets.get(dest);
            if (sel) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.5));
                Color glow = locked ? new Color(170, 60, 60) : new Color(255, 215, 0);
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), (int) (120 + pulse * 120)));
                g2.fill(new Ellipse2D.Double(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2));
            }
            Color planetBase = locked ? lerpColor(p.base, Color.DARK_GRAY, 0.75f) : p.base;
            Color planetAccent = locked ? lerpColor(p.accent, Color.DARK_GRAY, 0.75f) : p.accent;
            if (p.ringed) {
                double tilt = Scenery.ringTilt(dest);
                g2.setColor(planetAccent);
                g2.setStroke(new BasicStroke(2f));
                g2.rotate(tilt, cx, cy);
                g2.draw(new Ellipse2D.Double(cx - r * 1.5, cy - r * 0.35, r * 3, r * 0.7));
                g2.rotate(-tilt, cx, cy);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.setColor(planetBase);
            g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            if (!locked) game.scenery.drawPlanetFeatures(g2, dest, cx, cy, r, p.base, p.accent);

            g2.setColor(locked ? new Color(100, 100, 100) : sel ? Color.WHITE : new Color(160, 160, 160));
            g2.setFont(font(Font.BOLD, sel ? 15 : 12));
            game.drawCentered(g2, p.name, cx, cy + r + 20);

            PlanetRecords.Medal medal = game.planetRecords.shown(dest, game.highestLevelReached);
            if (medal != PlanetRecords.Medal.NONE) drawMedal(g2, medal, cx, cy - r - 16, sel ? 8 : 6);

            int startLevel = PLANET_START_LEVEL[dest];
            g2.setFont(font(Font.BOLD, sel ? 13 : 10));
            if (locked) {
                g2.setColor(new Color(150, 90, 90));
                game.drawCentered(g2, "LOCKED", cx, cy + r + (sel ? 38 : 34));
            } else {
                g2.setColor(DIFFICULTY_COLORS[sectorForLevel(startLevel)]);
                game.drawCentered(g2, DIFFICULTY_NAMES[sectorForLevel(startLevel)], cx, cy + r + (sel ? 38 : 34));
            }
        }

        int chosenDest = destinationAtSlot(game.levelSelectIndex);
        Planet chosen = game.destinationPlanet(chosenDest);
        boolean chosenLocked = game.isDestinationLocked(chosenDest);
        int chosenStart = game.destinationStartLevel(chosenDest);

        // The medal held here, and what the next one asks for.
        if (chosenDest != STAR_DEST) {
            PlanetRecords.Medal held = game.planetRecords.shown(chosenDest, game.highestLevelReached);
            g2.setFont(font(Font.BOLD, 13));
            String medalText = held == PlanetRecords.Medal.NONE ? "" : held.label + " medal  •  ";
            String goal = medalText + PlanetRecords.nextGoal(held);
            g2.setColor(held == PlanetRecords.Medal.NONE ? new Color(150, 160, 180) : held.color);
            game.drawCentered(g2, goal, GAME_WIDTH / 2, GAME_HEIGHT / 2 + 80);
        }

        // How the player has done in the topics this destination asks, so they choose knowing.
        g2.setFont(font(Font.PLAIN, 13));
        String mastery = game.destinationMasteryLine(chosenDest);
        String focus = chosenDest == STAR_DEST ? null : QuestionFile.planetFocus.get(chosenDest);
        if (focus != null) mastery = "Topic focus: " + focus + "   •   " + mastery;
        while (g2.getFontMetrics().stringWidth(mastery) > GAME_WIDTH - 60 && mastery.length() > 4) {
            mastery = mastery.substring(0, mastery.length() - 4) + "...";
        }
        g2.setColor(new Color(150, 200, 230));
        game.drawCentered(g2, mastery, GAME_WIDTH / 2, GAME_HEIGHT / 2 + 100);

        g2.setColor(chosenLocked ? new Color(220, 90, 90) : Color.YELLOW);
        g2.setFont(font(Font.BOLD, 22));
        String caption = chosenLocked
            ? "LOCKED - reach Level " + chosenStart + " through play to unlock"
            : "Start at Level " + chosenStart + " (" + chosen.name + ")";
        game.drawCentered(g2, caption, GAME_WIDTH / 2, GAME_HEIGHT / 2 + 130);

        g2.setFont(font(Font.BOLD, 14));
        g2.setColor(chosenLocked ? new Color(130, 120, 130) : lerpColor(chosen.base, Color.WHITE, 0.4f));
        game.drawCentered(g2, chosen.traitName, GAME_WIDTH / 2, GAME_HEIGHT / 2 + 155);
        g2.setFont(font(Font.PLAIN, 14));
        g2.setColor(new Color(185, 195, 210));
        game.drawCentered(g2, chosen.traitDesc, GAME_WIDTH / 2, GAME_HEIGHT / 2 + 175);

        // A finished planet can be replayed alone for a better medal.
        boolean challenge = game.canChallenge(chosenDest);
        if (challenge) drawChallengeButton(g2, game.planetRecords.shown(chosenDest, game.highestLevelReached));

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 16));
        game.drawCentered(g2, challenge
            ? "Left/Right or click to choose, ENTER to fly on from here, C for a medal challenge, ESC to go back"
            : "Left/Right or click to choose, ENTER to start, ESC or right-click to go back", GAME_WIDTH / 2, GAME_HEIGHT - 40);
    }

    /** Where the Medal Challenge button sits on Level Select, for drawing and for the mouse alike. */
    static final Rectangle2D CHALLENGE_BUTTON = new Rectangle2D.Double(GAME_WIDTH - 196, 362, 180, 30);

    /** A gold-edged pill offering the medal challenge, lit while the mouse is over it. */
    void drawChallengeButton(Graphics2D g2, PlanetRecords.Medal held) {
        Rectangle2D b = CHALLENGE_BUTTON;
        boolean hover = b.contains(game.mouseGX, game.mouseGY);
        g2.setColor(hover ? new Color(90, 70, 20, 235) : new Color(40, 34, 16, 220));
        g2.fill(new RoundRectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 15, 15));
        g2.setColor(new Color(255, 205, 60, hover ? 255 : 190));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 15, 15));
        g2.setStroke(new BasicStroke(1f));
        drawMedal(g2, held, b.getX() + 18, b.getCenterY() + 3, 6);
        g2.setFont(font(Font.BOLD, 14));
        g2.setColor(new Color(255, 225, 120));
        g2.drawString("Medal Challenge  (C)", (int) b.getX() + 32, (int) b.getCenterY() + 5);
    }

    void drawPausedScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 48));
        game.drawCentered(g2, "PAUSED", GAME_WIDTH / 2, GAME_HEIGHT / 2 - 60);

        game.drawSelectableList(g2, game.pauseItems, game.pauseIndex, GAME_HEIGHT / 2, 48, 28, 22);

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 15));
        game.drawCentered(g2, "W/S or Up/Down to choose, ENTER to select, ESC to resume", GAME_WIDTH / 2, GAME_HEIGHT / 2 + 120);
    }

    void drawGameOverScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 195));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.RED);
        g2.setFont(font(Font.BOLD, 54));
        game.drawCentered(g2, "GAME OVER", GAME_WIDTH / 2, GAME_HEIGHT / 2 - 40);

        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.PLAIN, 22));
        game.drawCentered(g2, "Score: " + game.score + "   Level Reached: " + game.level, GAME_WIDTH / 2, GAME_HEIGHT / 2 + 2);

        String weak = game.weakestTopic();
        if (weak != null) {
            g2.setFont(font(Font.BOLD, 15));
            FontMetrics wfm = g2.getFontMetrics();
            String text = "Weakest topic:  " + weak + "  (" + game.weakestTopicMisses() + " missed)";
            int boxW = wfm.stringWidth(text) + 30;
            int boxY = GAME_HEIGHT / 2 + 14;
            g2.setColor(new Color(60, 26, 26, 215));
            g2.fillRoundRect(GAME_WIDTH / 2 - boxW / 2, boxY, boxW, 26, 12, 12);
            g2.setColor(new Color(235, 130, 110));
            g2.drawRoundRect(GAME_WIDTH / 2 - boxW / 2, boxY, boxW, 26, 12, 12);
            game.drawCentered(g2, text, GAME_WIDTH / 2, boxY + 18);
        }

        game.drawSelectableList(g2, game.gameOverItems(), game.gameOverIndex, GAME_HEIGHT / 2 + 74, 34, 21, 17);

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 13));
        game.drawCentered(g2, "W/S or Up/Down to choose, ENTER to select", GAME_WIDTH / 2, GAME_HEIGHT - 16);
    }

    String[] howToLines(int page) {
        return switch (page) {
            case 0 -> new String[]{
                    KeyEvent.getKeyText(game.keyLeft) + " / " + KeyEvent.getKeyText(game.keyRight)
                        + "  (or the arrow keys)  steer the rocket left and right.",
                    KeyEvent.getKeyText(game.keyShoot) + "  fires a shot. Destroy the asteroid carrying the",
                    "correct answer - or simply fly into it.",
                    "",
                    KeyEvent.getKeyText(game.keyBoost) + " (or Up)  boosts: you climb, but the asteroids speed up.",
                    KeyEvent.getKeyText(game.keySlow) + " (or Down)  slows: the asteroids ease off, but the timer drains",
                    "twice as fast.",
                    "",
                    "Let the correct answer fall past you and you lose a heart,",
                    "so do not wait too long.",
                    "",
                    "TAB hides the question panel  -  ESC pauses  -  F11 fullscreen.",
                    "An Xbox-style controller works too - see Settings > Controls."
                };
            case 1 -> new String[]{
                    "Each correct answer scores:",
                    "",
                    "    ( 10 x level  +  2 per second left )  x  boost",
                    "",
                    "Answering quickly is worth real points, and answering while",
                    "boosting multiplies the whole thing by up to 2x. Taking the",
                    "risk is how big scores happen.",
                    "",
                    "Five correct answers in a row restores one lost heart.",
                    "",
                    "Get one wrong and the question comes straight back. Miss it",
                    "again and the right answer glows, with an explanation of why -",
                    "but shooting the glow scores half and earns no streak.",
                    "It also returns later in the run so it sticks."
                };
            default -> new String[]{
                    "Shoot or collect the glowing gems:",
                    "",
                    "    SHIELD   absorbs one wrong answer - no heart lost",
                    "    FREEZE   stops the asteroids and the timer for 4 seconds",
                    "    50/50    destroys two of the wrong answers",
                    "",
                    "Every planet plays differently. Mercury shortens the timer,",
                    "Jupiter drags asteroids down faster, Uranus takes away your",
                    "grip, Neptune blows you sideways, and Venus hides the field.",
                    "",
                    "Check a planet's trait on the Select Level screen before you",
                    "commit to it."
                };
        };
    }

    void drawHowToPlayScreen(Graphics2D g2) {
        g2.setColor(new Color(6, 8, 18, 225));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 32));
        game.drawCentered(g2, "HOW TO PLAY", GAME_WIDTH / 2, 52);

        g2.setColor(new Color(255, 215, 0));
        g2.setFont(font(Font.BOLD, 18));
        game.drawCentered(g2, HOW_TO_TITLES[game.howToPage], GAME_WIDTH / 2, 82);

        int panelX = 90, panelW = GAME_WIDTH - 180;
        int panelY = 100, panelH = GAME_HEIGHT - 100 - 74;
        g2.setColor(new Color(10, 14, 24, 215));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g2.setColor(new Color(110, 190, 255, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(font(Font.PLAIN, 16));
        FontMetrics fm = g2.getFontMetrics();
        int y = panelY + 32;
        for (String line : howToLines(game.howToPage)) {
            boolean emphasis = line.startsWith("    ");
            g2.setFont(font(emphasis ? Font.BOLD : Font.PLAIN, 16));
            g2.setColor(emphasis ? new Color(140, 225, 255) : new Color(222, 230, 242));
            g2.drawString(line, panelX + 34, y);
            y += fm.getHeight() + 2;
        }

        for (int i = 0; i < HOW_TO_PAGES; i++) {
            int dx = GAME_WIDTH / 2 - (HOW_TO_PAGES * 18) / 2 + i * 18;
            g2.setColor(i == game.howToPage ? new Color(255, 215, 0) : new Color(90, 100, 120));
            g2.fill(new Ellipse2D.Double(dx, GAME_HEIGHT - 58, 9, 9));
        }

        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 14));
        game.drawCentered(g2, "Left/Right or click to turn the page  " + '•' + "  F for Flight School  " + '•' + "  ENTER or ESC to go back", GAME_WIDTH / 2, GAME_HEIGHT - 26);
    }
}
