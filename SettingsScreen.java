package rocketlearn;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.*;
import java.util.Arrays;
import java.util.List;

import static rocketlearn.RocketLearn.*;

/**
 * The Settings and Controls screens: drawing them, the volume sliders,
 * and changing a setting from the keyboard, mouse or pad.
 */
class SettingsScreen {

    final RocketLearn game;

    SettingsScreen(RocketLearn game) { this.game = game; }

    // ---------------------------------------------------------------- groups

    /** Every row a settings page can hold. */
    enum Row {
        LIVES, TIMER, EXPLAIN, ASSIST, SHIP, TRAIL,
        SOUND, MUSIC, MASTER_VOLUME, MUSIC_VOLUME, EFFECTS_VOLUME,
        GRAPHICS, FRAME_RATE, VSYNC,
        MOTION, TEXT_SIZE, CONTRAST, READ_ALOUD,
        RESET, BACK;

        boolean isValue() { return this != RESET && this != BACK; }

        boolean isSlider() { return this == MASTER_VOLUME || this == MUSIC_VOLUME || this == EFFECTS_VOLUME; }
    }

    /**
     * The settings, grouped so each page is short and holds one kind of
     * thing. Every page ends with its own Reset, which touches only that
     * group, and Back to the hub.
     */
    enum Group {
        GAMEPLAY("Gameplay", Row.LIVES, Row.TIMER, Row.EXPLAIN, Row.ASSIST, Row.SHIP, Row.TRAIL),
        AUDIO("Audio", Row.SOUND, Row.MUSIC, Row.MASTER_VOLUME, Row.MUSIC_VOLUME, Row.EFFECTS_VOLUME),
        GRAPHICS("Graphics", Row.GRAPHICS, Row.FRAME_RATE, Row.VSYNC),
        ACCESSIBILITY("Accessibility", Row.MOTION, Row.TEXT_SIZE, Row.CONTRAST, Row.READ_ALOUD);

        final String title;
        final Row[] rows;

        Group(String title, Row... values) {
            this.title = title;
            rows = Arrays.copyOf(values, values.length + 2);
            rows[values.length] = Row.RESET;
            rows[values.length + 1] = Row.BACK;
        }
    }

    /** Hub rows after one per group. */
    static final int HUB_CONTROLS = Group.values().length, HUB_RESET = HUB_CONTROLS + 1,
        HUB_BACK = HUB_RESET + 1, HUB_ROWS = HUB_BACK + 1;

    Row[] rows() { return game.settingsGroup.rows; }

    /** The row at a place on the current page, or null past its end. */
    Row rowAt(int i) { return i >= 0 && i < rows().length ? rows()[i] : null; }

    Row currentRow() { return rowAt(game.settingsIndex); }

    /** Opens a group's page on its first row. */
    void open(Group group) {
        game.settingsGroup = group;
        game.settingsIndex = 0;
        game.resetArmed = false;
        game.state = State.SETTINGS;
    }

    /** Opens the page that holds a row, with that row selected. */
    void open(Row row) {
        for (Group group : Group.values()) {
            int i = Arrays.asList(group.rows).indexOf(row);
            if (i >= 0 && row.isValue()) {
                open(group);
                game.settingsIndex = i;
                return;
            }
        }
    }

    /** Puts one group back to its first-run values, then saves. The ship paint is the player's to keep. */
    void resetGroup(Group group) {
        switch (group) {
            case GAMEPLAY -> {
                game.startingHearts = MIN_HEARTS;
                game.fastTimer = false;
                game.explainMode = ExplainMode.HINT;
                game.adaptiveHelp = true;
            }
            case AUDIO -> {
                game.soundOn = true;
                game.musicOn = true;
                game.masterVolume = 100;
                game.musicVolume = 100;
                game.effectsVolume = 100;
                game.syncVolumes();
                game.syncMusic();
            }
            case GRAPHICS -> {
                game.graphics = GraphicsQuality.recommended();
                game.frameRate = RocketLearn.recommendedFrameRate();
                if (!game.vsync) game.setVsync(true);
            }
            case ACCESSIBILITY -> {
                game.reduceMotion = false;
                game.largeText = false;
                game.highContrast = false;
                game.readAloud = READ_OFF;
            }
        }
        game.saveFile.saveProgress();
    }

    /** Every group back to its first-run values. The ship paint and the keys are left alone. */
    void resetSettingsToDefaults() {
        for (Group group : Group.values()) resetGroup(group);
    }

    /** What the hub's Reset All does: every group and the keys. */
    void resetEverything() {
        resetSettingsToDefaults();
        resetControlsToDefaults();
    }

    void resetControlsToDefaults() {
        game.keyLeft = KeyEvent.VK_A;
        game.keyRight = KeyEvent.VK_D;
        game.keyShoot = KeyEvent.VK_SPACE;
        game.keyBoost = KeyEvent.VK_W;
        game.keySlow = KeyEvent.VK_S;
        game.saveFile.saveProgress();
    }

    int getControlKey(int idx) {
        return switch (idx) {
            case 0 -> game.keyLeft;
            case 1 -> game.keyRight;
            case 2 -> game.keyShoot;
            case 3 -> game.keyBoost;
            default -> game.keySlow;
        };
    }

    void setControlKey(int idx, int keyCode) {
        switch (idx) {
            case 0: game.keyLeft = keyCode; break;
            case 1: game.keyRight = keyCode; break;
            case 2: game.keyShoot = keyCode; break;
            case 3: game.keyBoost = keyCode; break;
            default: game.keySlow = keyCode; break;
        }
    }

    static boolean isSliderRow(int[] sliders, int i) {
        return sliders != null && i < sliders.length && sliders[i] >= 0;
    }

    // Slider geometry, shared by drawing and the mouse so they always agree.
    static final int SLIDER_X0 = GAME_WIDTH / 2 - 10, SLIDER_X1 = GAME_WIDTH / 2 + 170;
    static final int SLIDER_ROW_W = 430;
    static final Color SLIDER_ACCENT = new Color(150, 105, 235);

    /**
     * One volume row: the label, a speaker whose waves follow the level, a
     * track filled up to a round knob, and the percentage. The font and
     * colour for the label are already set by the list.
     */
    void drawVolumeSlider(Graphics2D g2, String label, int value, boolean muted, boolean sel, int cy) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, GAME_WIDTH / 2 - 62 - fm.stringWidth(label), cy);

        int my = cy - 7;   // the row's visual middle, a little above the baseline
        Stroke oldStroke = g2.getStroke();
        Color iconColor = sel ? new Color(230, 230, 238) : new Color(185, 185, 195);

        // The speaker: a box and a flared cone.
        double ix = GAME_WIDTH / 2 - 44;
        Path2D speaker = new Path2D.Double();
        speaker.moveTo(ix - 8, my - 4);
        speaker.lineTo(ix - 3, my - 4);
        speaker.lineTo(ix + 3, my - 9);
        speaker.lineTo(ix + 3, my + 9);
        speaker.lineTo(ix - 3, my + 4);
        speaker.lineTo(ix - 8, my + 4);
        speaker.closePath();
        g2.setColor(iconColor);
        g2.fill(speaker);

        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (muted || value == 0) {
            g2.draw(new Line2D.Double(ix + 8, my - 4, ix + 16, my + 4));
            g2.draw(new Line2D.Double(ix + 8, my + 4, ix + 16, my - 4));
        } else {
            int waves = value < 34 ? 1 : value < 67 ? 2 : 3;
            for (int w = 0; w < waves; w++) {
                double r = 5 + w * 4.5;
                g2.draw(new Arc2D.Double(ix + 3 - r, my - r, r * 2, r * 2, -45, 90, Arc2D.OPEN));
            }
        }

        // The track, filled up to the knob.
        double knobX = SLIDER_X0 + (SLIDER_X1 - SLIDER_X0) * value / 100.0;
        Color accent = muted ? new Color(120, 120, 130)
            : sel ? SLIDER_ACCENT : lerpColor(SLIDER_ACCENT, new Color(90, 90, 100), 0.35f);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(85, 88, 102));
        g2.draw(new Line2D.Double(SLIDER_X0, my, SLIDER_X1, my));
        if (value > 0) {
            g2.setColor(accent);
            g2.draw(new Line2D.Double(SLIDER_X0, my, knobX, my));
        }
        g2.setStroke(oldStroke);

        double kr = sel ? 8 : 7;
        g2.setColor(accent);
        g2.fill(new Ellipse2D.Double(knobX - kr, my - kr, kr * 2, kr * 2));
        if (sel) {
            g2.setColor(Color.WHITE);
            g2.draw(new Ellipse2D.Double(knobX - kr, my - kr, kr * 2, kr * 2));
        }

        g2.setFont(font(Font.PLAIN, 15));
        g2.setColor(sel ? Color.YELLOW : new Color(200, 200, 210));
        g2.drawString(value + "%", SLIDER_X1 + 16, cy - 1);
    }

    /** The volume a click at gx asks for, or -1 when it is off the track. */
    static int sliderValueAt(double gx) {
        if (gx < SLIDER_X0 - 12 || gx > SLIDER_X1 + 12) return -1;
        return clampInt((int) Math.round((gx - SLIDER_X0) * 100.0 / (SLIDER_X1 - SLIDER_X0)), 0, 100);
    }

    /** Where a settings page's list starts, and its spacing; a page holds at most seven rows. */
    static final int LIST_TOP = GAME_HEIGHT / 2 - 112, ROW_GAP = 34;
    /** The hub's list, a little roomier. */
    static final int HUB_TOP = GAME_HEIGHT / 2 - 118, HUB_GAP = 38;

    /** A short line of a group's current values, shown on the hub. */
    String summary(Group group) {
        return switch (group) {
            case GAMEPLAY -> game.startingHearts + " lives  •  " + (game.fastTimer ? "Fast" : "Normal") + " timer  •  "
                + "Explanations: " + game.explainMode.label + "  •  Ship: " + game.currentSkin().name;
            case AUDIO -> !game.soundOn ? "Sound is off"
                : "Master " + game.masterVolume + "%  •  Music " + (game.musicOn ? game.musicVolume + "%" : "off")
                    + "  •  Effects " + game.effectsVolume + "%";
            case GRAPHICS -> "Quality: " + game.graphics.label + "  •  Frame rate: " + game.frameRateLabel()
                + "  •  V-Sync: " + (game.vsync ? "On" : "Off");
            case ACCESSIBILITY -> "Motion: " + (game.reduceMotion ? "Reduced" : "Full")
                + "  •  Text size: " + (game.largeText ? "Large" : "Normal");
        };
    }

    void drawSettingsMenu(Graphics2D g2) {
        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 44));
        game.drawCentered(g2, "SETTINGS", GAME_WIDTH / 2, GAME_HEIGHT / 2 - 170);

        Group[] groups = Group.values();
        String[] labels = new String[HUB_ROWS];
        for (int i = 0; i < groups.length; i++) labels[i] = groups[i].title;
        labels[HUB_CONTROLS] = "Controls";
        labels[HUB_RESET] = game.resetArmed && game.settingsMenuIndex == HUB_RESET ? "Reset All - press again" : "Reset All to Defaults";
        labels[HUB_BACK] = "Back";
        game.drawSelectableList(g2, labels, game.settingsMenuIndex, HUB_TOP, HUB_GAP, 22, 18);

        // Until a new paint or trail has been looked at, the page that holds them says so.
        if ((game.rewards.skinsUnseen() || game.rewards.trailsUnseen()) && game.settingsMenuIndex != Group.GAMEPLAY.ordinal()) {
            FontMetrics rowFm = g2.getFontMetrics(font(Font.BOLD, 18));
            int rowY = HUB_TOP + Group.GAMEPLAY.ordinal() * HUB_GAP;
            game.drawNewBadge(g2, GAME_WIDTH / 2 + rowFm.stringWidth(labels[Group.GAMEPLAY.ordinal()]) / 2 + 12, rowY - 15);
        }

        int sel = game.settingsMenuIndex;
        String hint = sel < groups.length ? summary(groups[sel])
            : sel == HUB_CONTROLS ? "Rebind the keyboard keys, and see the controller's buttons"
            : sel == HUB_RESET ? (game.resetArmed ? "ENTER again to reset every group and the keys - your ship paint is kept"
                : "Resets every group and the keys - keeps ship paint")
            : "ENTER to return to the Main Menu";
        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 16));
        game.drawCentered(g2, hint, GAME_WIDTH / 2, GAME_HEIGHT - 40);
        game.drawCentered(g2, "W/S or Up/Down to choose  •  ENTER or click to open  •  ESC to go back", GAME_WIDTH / 2, GAME_HEIGHT - 20);
    }

    String label(Row row) {
        return switch (row) {
            case LIVES -> "Lives: " + game.startingHearts;
            case TIMER -> "Timer: " + (game.fastTimer ? "Fast" : "Normal");
            case ASSIST -> "Adaptive Help: " + (game.adaptiveHelp ? "On" : "Off");
            case EXPLAIN -> "Explanations: " + game.explainMode.label;
            case SHIP -> "Ship: " + game.currentSkin().name;
            case TRAIL -> "Trail: " + game.currentTrail().name;
            case SOUND -> "Sound: " + (game.soundOn ? "On" : "Off");
            case MUSIC -> "Music: " + (game.musicOn ? "On" : "Off");
            case MASTER_VOLUME -> "Master Volume";
            case MUSIC_VOLUME -> "Music Volume";
            case EFFECTS_VOLUME -> "Effects Volume";
            case GRAPHICS -> "Quality: " + game.graphics.label;
            case FRAME_RATE -> "Frame Rate: " + game.frameRateLabel();
            case VSYNC -> "V-Sync: " + (game.vsync ? "On" : "Off");
            case MOTION -> "Motion: " + (game.reduceMotion ? "Reduced" : "Full");
            case TEXT_SIZE -> "Text Size: " + (game.largeText ? "Large" : "Normal");
            case CONTRAST -> "High Contrast: " + (game.highContrast ? "On" : "Off");
            case READ_ALOUD -> "Read Aloud: " + READ_LABELS[game.readAloud];
            case RESET -> game.resetArmed && currentRow() == Row.RESET ? "Reset " + game.settingsGroup.title + " - press again"
                : "Reset " + game.settingsGroup.title;
            case BACK -> "Back";
        };
    }

    String hint(Row row) {
        return switch (row) {
            case LIVES -> "Left/Right to change lives (" + MIN_HEARTS + "-" + MAX_HEARTS + ")";
            case TIMER -> "Left/Right to toggle Fast/Normal timer";
            case ASSIST -> "On: more time and slower rocks on topics you find hard, and on your last heart";
            case EXPLAIN -> game.explainMode.hint;
            case SHIP -> "Left/Right to change paint - " + game.rewards.unlockedSkins().size() + " of " + ShipSkin.ALL.size() + " unlocked";
            case TRAIL -> "Left/Right to change engine trail - " + game.rewards.unlockedTrails().size() + " of " + EngineTrail.ALL.size() + " unlocked";
            case SOUND -> "Left/Right to mute or unmute all audio";
            case MUSIC -> game.soundOn ? "Left/Right to turn the background music on or off"
                : "Sound is off, so the music is silent either way";
            case MASTER_VOLUME -> "Drag or Left/Right to set the overall loudness - scales music and effects";
            case MUSIC_VOLUME -> "Drag or Left/Right to set the music loudness";
            case EFFECTS_VOLUME -> "Drag or Left/Right to set the sound effect loudness";
            case GRAPHICS -> game.graphics.label + ": " + game.graphics.hint;
            case FRAME_RATE -> game.frameRate == FRAME_RATE_UNLIMITED
                ? "No cap - as many frames as the computer can draw; runs it hot and drains a laptop battery"
                : game.vsync && game.frameRate != 0
                ? "Frames per second - with V-Sync on, fullscreen never goes past the monitor's rate"
                : "Frames per second - higher is smoother but works the computer harder; 60 suits slower PCs";
            case VSYNC -> game.vsync
                ? "On: fullscreen waits for each monitor refresh - no tearing, frames capped at the refresh rate"
                : "Off: frames are shown as soon as they are drawn - lets 144, 240 and Unlimited run past the monitor";
            case MOTION -> "Reduced turns off screen shake and softens the full-screen flashes";
            case TEXT_SIZE -> "Large makes questions, answers and explanations easier to read";
            case CONTRAST -> "Solid panels behind questions and answers, and bolder asteroid outlines";
            case READ_ALOUD -> "The computer's voice reads each question - and its answers, left to right - and every explanation";
            case RESET -> game.resetArmed ? "ENTER again to put only the " + game.settingsGroup.title + " settings back"
                : "Puts only the " + game.settingsGroup.title + " settings back to their defaults";
            case BACK -> "ENTER to return to Settings";
        };
    }

    void drawSettingsScreen(Graphics2D g2) {
        Group group = game.settingsGroup;
        g2.setColor(new Color(150, 165, 190));
        g2.setFont(font(Font.BOLD, 16));
        game.drawCentered(g2, "SETTINGS", GAME_WIDTH / 2, GAME_HEIGHT / 2 - 200);
        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 40));
        game.drawCentered(g2, group.title.toUpperCase(), GAME_WIDTH / 2, GAME_HEIGHT / 2 - 160);

        Row[] rows = rows();
        String[] labels = new String[rows.length];
        // A volume row draws as a slider; a muted channel greys its slider.
        int[] sliders = new int[rows.length];
        boolean[] muted = new boolean[rows.length];
        Arrays.fill(sliders, -1);
        for (int i = 0; i < rows.length; i++) {
            labels[i] = label(rows[i]);
            switch (rows[i]) {
                case MASTER_VOLUME -> { sliders[i] = game.masterVolume; muted[i] = !game.soundOn; }
                case MUSIC_VOLUME -> { sliders[i] = game.musicVolume; muted[i] = !game.soundOn || !game.musicOn; }
                case EFFECTS_VOLUME -> { sliders[i] = game.effectsVolume; muted[i] = !game.soundOn; }
                default -> { }
            }
        }
        game.drawSelectableList(g2, labels, game.settingsIndex, LIST_TOP, ROW_GAP, 21, 18, sliders, muted);

        Row current = currentRow();
        // Until a new paint or trail has been looked at, its row says so.
        int shipAt = Arrays.asList(rows).indexOf(Row.SHIP), trailAt = Arrays.asList(rows).indexOf(Row.TRAIL);
        FontMetrics rowFm = g2.getFontMetrics(font(Font.BOLD, 18));
        if (game.rewards.skinsUnseen() && shipAt >= 0 && current != Row.SHIP) {
            game.drawNewBadge(g2, GAME_WIDTH / 2 + rowFm.stringWidth(labels[shipAt]) / 2 + 12, LIST_TOP + shipAt * ROW_GAP - 15);
        }
        if (game.rewards.trailsUnseen() && trailAt >= 0 && current != Row.TRAIL) {
            game.drawNewBadge(g2, GAME_WIDTH / 2 + rowFm.stringWidth(labels[trailAt]) / 2 + 12, LIST_TOP + trailAt * ROW_GAP - 15);
        }

        // A live preview of the chosen ship beside the list.
        if (current == Row.SHIP) {
            game.ship.drawShipPreview(g2, game.shipSkinIndex, GAME_WIDTH - 130, GAME_HEIGHT / 2 + 10, 1.5);

            // The paint's laser, mid-flight above the ship.
            java.awt.geom.AffineTransform oldTx = g2.getTransform();
            g2.translate(GAME_WIDTH - 130, GAME_HEIGHT / 2 - 72);
            g2.scale(1.5, 1.5);
            game.ship.drawLaser(g2, game.currentSkin(), 0, 0, game.graphics.glows);
            g2.setTransform(oldTx);

            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(new Color(150, 165, 190));
            game.drawCentered(g2, game.currentSkin().unlockLabel(), GAME_WIDTH - 130, GAME_HEIGHT / 2 + 108);
            g2.setColor(game.currentSkin().laserGlow);
            game.drawCentered(g2, "Laser: " + game.currentSkin().laser.label, GAME_WIDTH - 130, GAME_HEIGHT / 2 + 124);
        } else if (current == Row.TRAIL) {
            EngineTrail trail = game.currentTrail();
            // A still of the trail: a fan of its particles under the ship's flame.
            if (trail.kind != EngineTrail.Kind.NONE) {
                int baseX = GAME_WIDTH - 130, baseY = GAME_HEIGHT / 2 + 70;
                for (int i = 0; i < 16; i++) {
                    double spread = trail.kind == EngineTrail.Kind.SPARKS ? 30 : 18;
                    double px = baseX + Math.sin(i * 2.3) * spread * (i / 16.0 + 0.2);
                    double py = baseY + i * 3.2;
                    double s = 6 - i * 0.3;
                    Color c = trail.particles[i % trail.particles.length];
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 235 - i * 12));
                    g2.fill(new Ellipse2D.Double(px - s / 2, py - s / 2, s, s));
                }
            }
            game.ship.drawShipPreview(g2, game.shipSkinIndex, GAME_WIDTH - 130, GAME_HEIGHT / 2 + 10, 1.5);
            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(new Color(150, 165, 190));
            game.drawCentered(g2, trail.unlockLabel(), GAME_WIDTH - 130, GAME_HEIGHT / 2 + 140);
        }
        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 16));
        game.drawCentered(g2, current == null ? "" : hint(current), GAME_WIDTH / 2, GAME_HEIGHT - 40);
        boolean onSlider = current != null && current.isSlider();
        game.drawCentered(g2, onSlider
            ? "W/S or Up/Down to choose  •  Click, drag or scroll the slider  •  ESC to go back"
            : "W/S or Up/Down to choose  •  Click to raise, right-click to lower  •  ESC to go back", GAME_WIDTH / 2, GAME_HEIGHT - 20);
    }

    void drawControlsScreen(Graphics2D g2) {
        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 40));
        game.drawCentered(g2, "CONTROLS", GAME_WIDTH / 2, GAME_HEIGHT / 2 - 190);

        String[] labels = new String[game.controlsRows()];
        for (int i = 0; i < game.controlActionNames.length; i++) {
            labels[i] = game.controlActionNames[i] + ": " + KeyEvent.getKeyText(getControlKey(i));
        }
        boolean armedHere = game.resetArmed && game.controlsIndex == game.controlsResetRow();
        labels[game.controlsResetRow()] = armedHere ? "Reset to Defaults - press again" : "Reset to Defaults";
        labels[labels.length - 1] = "Back";

        game.drawSelectableList(g2, labels, game.controlsIndex, GAME_HEIGHT / 2 - 125, 38, 23, 18);

        // The controller, when one is plugged in, and what its buttons do.
        g2.setFont(font(Font.PLAIN, 13));
        if (Gamepad.connected()) {
            g2.setColor(new Color(120, 225, 150));
            game.drawCentered(g2, "Controller connected:  stick/D-pad steer  •  RT boost, LT slow  •  A shoot/select  •  B back  •  Start pause",
                GAME_WIDTH / 2, GAME_HEIGHT - 66);
        } else {
            g2.setColor(new Color(130, 140, 160));
            game.drawCentered(g2, Gamepad.available() ? "No controller connected - plug in an Xbox-style controller to use one"
                : "Controllers are not supported on this system", GAME_WIDTH / 2, GAME_HEIGHT - 66);
        }

        g2.setColor(game.awaitingKeybind ? Color.YELLOW : new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 16));
        String hint = game.awaitingKeybind ? "Press any key to bind... (ESC or click to cancel)"
            : game.controlsIndex == labels.length - 1 ? "ENTER to return to Settings"
            : game.controlsIndex == game.controlsResetRow() ? (game.resetArmed ? "ENTER again to restore A/D, Space, W/S"
                : "Puts every key back to A/D steer, Space shoot, W/S thrusters")
            : "ENTER to rebind  •  The arrow keys always fly the rocket too";
        game.drawCentered(g2, hint, GAME_WIDTH / 2, GAME_HEIGHT - 40);
        g2.setColor(new Color(180, 180, 180));
        game.drawCentered(g2, "W/S or Up/Down to choose  •  ESC to go back", GAME_WIDTH / 2, GAME_HEIGHT - 20);
    }

    /** Changes the selected row's value one step; rows without a value are left alone. */
    void adjustSetting(int dir) {
        Row row = currentRow();
        if (row == null || !row.isValue()) return;
        // Changing paint fires the new paint's laser instead, so it can be heard as well as seen.
        if (row != Row.SHIP) game.playSfx(Sfx.CONFIRM);
        switch (row) {
            case LIVES -> game.startingHearts = ((game.startingHearts - MIN_HEARTS + dir) % HEART_OPTIONS + HEART_OPTIONS) % HEART_OPTIONS + MIN_HEARTS;
            case TIMER -> game.fastTimer = !game.fastTimer;
            case ASSIST -> game.adaptiveHelp = !game.adaptiveHelp;
            case EXPLAIN -> game.explainMode = game.explainMode.step(dir);
            case SHIP -> {
                cycleShipSkin(dir);
                game.playSfx(Sfx.laserShot(game.currentSkin().laser));
            }
            case TRAIL -> cycleTrail(dir);
            case SOUND -> {
                game.soundOn = !game.soundOn;
                game.syncMusic();
            }
            case MUSIC -> {
                game.musicOn = !game.musicOn;
                game.syncMusic();
            }
            case MASTER_VOLUME, MUSIC_VOLUME, EFFECTS_VOLUME -> {
                int step = dir * VOLUME_STEP;
                switch (row) {
                    case MASTER_VOLUME -> game.masterVolume = clampInt(game.masterVolume + step, 0, 100);
                    case MUSIC_VOLUME -> game.musicVolume = clampInt(game.musicVolume + step, 0, 100);
                    default -> game.effectsVolume = clampInt(game.effectsVolume + step, 0, 100);
                }
                game.syncVolumes();
                // A tick at the new level, so the effect channels can be judged by
                // ear while setting them rather than only after leaving.
                if (row != Row.MUSIC_VOLUME) game.playSfx(Sfx.CLICK);
            }
            case GRAPHICS -> game.graphics = game.graphics.step(dir);
            case FRAME_RATE -> game.stepFrameRate(dir);
            case VSYNC -> game.setVsync(!game.vsync);
            case MOTION -> game.reduceMotion = !game.reduceMotion;
            case TEXT_SIZE -> game.largeText = !game.largeText;
            case CONTRAST -> game.highContrast = !game.highContrast;
            case READ_ALOUD -> {
                game.readAloud = ((game.readAloud + dir) % READ_LABELS.length + READ_LABELS.length) % READ_LABELS.length;
                if (game.readAloud != READ_OFF) Speech.say("Read aloud is on");
            }
            default -> { }
        }
        game.saveFile.saveProgress();
    }

    /** Whether a place on the current page holds a value to change. */
    boolean isValueIndex(int i) {
        Row row = rowAt(i);
        return row != null && row.isValue();
    }

    /** Whether a place on the current page holds a volume slider. */
    boolean isSliderIndex(int i) {
        Row row = rowAt(i);
        return row != null && row.isSlider();
    }

    /** The page row whose knob the mouse is holding, or -1. */
    int draggingSlider = -1;

    /** Keeps a drag that wanders past either end pinned to that end. */
    static double clampSliderX(double gx) { return clamp(gx, SLIDER_X0, SLIDER_X1); }

    /** Sets one volume row directly, as a click or drag on its track does. */
    void setVolumeRow(int index, int value) {
        Row row = rowAt(index);
        if (value < 0 || row == null || !row.isSlider()) return;
        switch (row) {
            case MASTER_VOLUME -> game.masterVolume = value;
            case MUSIC_VOLUME -> game.musicVolume = value;
            default -> game.effectsVolume = value;
        }
        game.syncVolumes();
    }

    /** Letting go saves the level and, for the effect channels, plays a tick at it. */
    void endSliderDrag() {
        if (draggingSlider < 0) return;
        if (rowAt(draggingSlider) != Row.MUSIC_VOLUME) game.playSfx(Sfx.CLICK);
        draggingSlider = -1;
        game.saveFile.saveProgress();
    }

    /** Steps through only the engine trails the player has earned. */
    void cycleTrail(int dir) {
        List<EngineTrail> trails = game.rewards.unlockedTrails();
        if (trails.isEmpty()) return;
        int pos = Math.max(0, trails.indexOf(game.currentTrail()));
        pos = ((pos + dir) % trails.size() + trails.size()) % trails.size();
        game.trailIndex = EngineTrail.ALL.indexOf(trails.get(pos));
    }

    /** Steps through only the skins the player has actually unlocked. */
    void cycleShipSkin(int dir) {
        List<ShipSkin> skins = game.rewards.unlockedSkins();
        if (skins.isEmpty()) return;
        int pos = skins.indexOf(game.currentSkin());
        if (pos < 0) pos = 0;
        pos = ((pos + dir) % skins.size() + skins.size()) % skins.size();
        game.shipSkinIndex = ShipSkin.ALL.indexOf(skins.get(pos));
    }
}
