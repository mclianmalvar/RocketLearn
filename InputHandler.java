package rocketlearn;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static rocketlearn.RocketLearn.*;

/**
 * Turns the keyboard, mouse and controller into game actions: what each
 * key, click, scroll and button does on every screen.
 */
class InputHandler implements KeyListener {

    final RocketLearn game;

    InputHandler(RocketLearn game) { this.game = game; }

    /** Whether a click at the mouse's position would do something on this screen. */
    boolean overClickable() {
        if (game.mouseGX < 0 || game.awaitingKeybind) return false;
        return switch (game.state) {
            case NAME_ENTRY -> nameEntryHit(game.mouseGX, game.mouseGY)[0] != -1;
            case LEVEL_SELECT -> game.planetIndexAt(game.mouseGX, game.mouseGY) >= 0
                || (MenuScreens.CHALLENGE_BUTTON.contains(game.mouseGX, game.mouseGY)
                    && game.canChallenge(destinationAtSlot(game.levelSelectIndex)));
            default -> !game.mouseInert() && game.listIndexAt(game.mouseGY) >= 0;
        };
    }

    boolean handCursorShown = false;

    /** Shows the pointing hand over anything clickable. Runs every frame, so it follows screen changes too. */
    void updateCursor() {
        boolean hand = overClickable();
        if (hand == handCursorShown) return;
        handCursorShown = hand;
        game.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    void onMouseMoved(MouseEvent e) {
        double gx = (e.getX() - game.viewOffX) / game.viewScale;
        double gy = (e.getY() - game.viewOffY) / game.viewScale;
        game.mouseGX = gx;
        game.mouseGY = gy;
        if (game.mouseInert()) return;

        if (game.state == State.LEVEL_SELECT) {
            int idx = game.planetIndexAt(gx, gy);
            if (idx >= 0 && idx != game.levelSelectIndex) {
                game.levelSelectIndex = idx;
                game.playSfx(Sfx.CLICK);
            }
            return;
        }
        int idx = game.listIndexAt(gy);
        if (idx >= 0 && idx != game.selectionForState()) {
            game.setSelectionForState(idx);
            game.playSfx(Sfx.CLICK);
        }
    }

    /**
     * Steps back one screen: what ESC does, and what a right-click does. Play
     * itself is left out, so a stray right-click never pauses a run.
     */
    void goBack() {
        switch (game.state) {
            case PAUSED -> { game.playSfx(Sfx.CONFIRM); game.state = State.PLAYING; game.syncMusic(); }
            case GAME_OVER, VICTORY -> game.returnToMenu();
            case REVIEW -> { game.playSfx(Sfx.CONFIRM); game.gameOverIndex = 0; game.state = State.GAME_OVER; }
            case HOW_TO_PLAY, HIGH_SCORES -> confirmCurrent();
            case LEVEL_SELECT, STUDY -> { game.playSfx(Sfx.CONFIRM); game.state = State.MENU; }
            case NOTEBOOK, TOPIC_STATS, CODEX -> { game.playSfx(Sfx.CONFIRM); game.state = State.STUDY; }
            case PILOTS -> game.pilots.keyPressed(KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
            case SETTINGS_MENU -> { game.playSfx(Sfx.CONFIRM); game.saveFile.saveProgress(); game.state = State.MENU; }
            case SETTINGS -> { game.playSfx(Sfx.CONFIRM); game.saveFile.saveProgress(); game.state = State.SETTINGS_MENU; }
            case CONTROLS -> {
                if (game.awaitingKeybind) { game.awaitingKeybind = false; game.playSfx(Sfx.CLICK); }
                else { game.playSfx(Sfx.CONFIRM); game.saveFile.saveProgress(); game.state = State.SETTINGS_MENU; }
            }
            default -> { }
        }
    }

    /**
     * Clicks on the screens with no list to pick from: each does what its
     * "go on" key does, so the mouse alone can get through the whole game.
     * Returns whether the click was used.
     */
    boolean clickThrough(boolean right) {
        switch (game.state) {
            case PLAYING -> {
                if (!right && game.bannerAwaitInput) game.dismissBanner();
                return true;   // never steer or pause from the mouse mid-run
            }
            case STAR -> {
                if (right) return true;
                if (!game.starEnding.starFinishedSpeaking()) game.starEnding.advanceStarSpeech();
                else confirmCurrent();
                return true;
            }
            case HOW_TO_PLAY -> {
                if (right) { goBack(); return true; }
                // Left-click reads on; past the last page it closes the guide.
                if (game.howToPage < HOW_TO_PAGES - 1) { game.howToPage++; game.playSfx(Sfx.CLICK); }
                else confirmCurrent();
                return true;
            }
            case REVIEW, NOTEBOOK -> {
                if (right || game.browserList().size() <= 1) { goBack(); return true; }
                browse(1);
                return true;
            }
            case CODEX -> {
                if (right) goBack(); else browseCodex(1);
                return true;
            }
            case HIGH_SCORES -> {
                goBack();
                return true;
            }
            case NAME_ENTRY -> {
                // Left-click works the boxes and SAVE; right-click does nothing,
                // so a stray one never throws away a high score.
                if (!right) game.nameEntryClick(game.mouseGX, game.mouseGY);
                return true;
            }
            case CONTROLS -> {
                if (game.awaitingKeybind) { goBack(); return true; }   // any click cancels the wait
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Scroll left over from earlier wheel events. A trackpad reports many tiny
     * fractions of a notch; stepping once per whole notch keeps a gentle swipe
     * from racing down a list.
     */
    double wheelCarry = 0;

    void onMouseWheel(double rotation) {
        if (game.state == State.PLAYING || game.state == State.STAR
            || game.state == State.HIGH_SCORES || game.awaitingKeybind) {
            wheelCarry = 0;
            return;
        }
        wheelCarry += rotation;
        while (Math.abs(wheelCarry) >= 1) {
            int dir = wheelCarry > 0 ? 1 : -1;   // scrolling down moves down the list
            wheelCarry -= dir;
            wheelStep(dir);
        }
    }

    /** The volume row whose slider the mouse is over, or -1. */
    int sliderRowUnderMouse() {
        if (game.state != State.SETTINGS) return -1;
        int row = game.listIndexAt(game.mouseGY);
        return game.settings.isSliderIndex(row) && SettingsScreen.sliderValueAt(game.mouseGX) >= 0 ? row : -1;
    }

    void wheelStep(int dir) {
        // Over a volume slider the wheel turns it, up for louder, rather than moving the list.
        int sliderRow = sliderRowUnderMouse();
        if (sliderRow >= 0) {
            game.settingsIndex = sliderRow;
            game.settings.adjustSetting(-dir);
            return;
        }
        switch (game.state) {
            case NAME_ENTRY -> game.nameEntryScroll(dir);
            case HOW_TO_PLAY -> {
                game.howToPage = ((game.howToPage + dir) % HOW_TO_PAGES + HOW_TO_PAGES) % HOW_TO_PAGES;
                game.playSfx(Sfx.CLICK);
            }
            case REVIEW, NOTEBOOK -> browse(dir);
            case CODEX -> browseCodex(dir);
            case LEVEL_SELECT -> {
                game.levelSelectIndex = ((game.levelSelectIndex + dir) % game.destinationCount() + game.destinationCount()) % game.destinationCount();
                game.playSfx(Sfx.CLICK);
            }
            default -> {
                // Anything drawn as a selectable list: step its selection.
                if (game.uiListState != game.state || game.uiListCount <= 0 || game.selectionForState() < 0) return;
                int n = game.uiListCount;
                game.setSelectionForState(((game.selectionForState() + dir) % n + n) % n);
                game.playSfx(Sfx.CLICK);
            }
        }
    }

    /** A medal challenge on the planet selected in Level Select, when it is one the player has finished. */
    void startSelectedChallenge() {
        int dest = destinationAtSlot(game.levelSelectIndex);
        if (!game.canChallenge(dest)) {
            game.playSfx(Sfx.CLICK);
            return;
        }
        game.playSfx(Sfx.CONFIRM);
        game.startChallenge(dest);
    }

    /** Steps the codex one planet either way along the route, wrapping at the ends. */
    void browseCodex(int dir) {
        int n = TRAVEL_ORDER.length;
        game.codexIndex = ((game.codexIndex + dir) % n + n) % n;
        game.playSfx(Sfx.CLICK);
    }

    /** Steps the question browser one page either way, wrapping at the ends. */
    void browse(int dir) {
        List<Question> list = game.browserList();
        if (list.isEmpty()) return;
        int n = list.size();
        if (game.state == State.NOTEBOOK) game.notebookIndex = ((game.notebookIndex + dir) % n + n) % n;
        else game.reviewIndex = ((game.reviewIndex + dir) % n + n) % n;
        game.playSfx(Sfx.CLICK);
    }

    void onMouseClicked(MouseEvent e) {
        boolean right = SwingUtilities.isRightMouseButton(e);
        game.mouseGX = (e.getX() - game.viewOffX) / game.viewScale;
        game.mouseGY = (e.getY() - game.viewOffY) / game.viewScale;
        if (!right && game.confirmBlocked()) return;
        if (clickThrough(right)) return;
        double gx = (e.getX() - game.viewOffX) / game.viewScale;
        double gy = (e.getY() - game.viewOffY) / game.viewScale;
        if (!right && game.state == State.MENU && PilotScreen.LABEL.contains(gx, gy)) {
            game.playSfx(Sfx.CONFIRM);
            game.pilots.open();
            return;
        }

        // A press on a volume track jumps the knob there and starts a drag.
        if (!right && game.state == State.SETTINGS) {
            int row = game.listIndexAt(gy);
            int v = game.settings.isSliderIndex(row) ? SettingsScreen.sliderValueAt(gx) : -1;
            if (v >= 0) {
                game.settingsIndex = row;
                game.resetArmed = false;
                game.settings.draggingSlider = row;
                game.settings.setVolumeRow(row, v);
                return;
            }
        }

        if (right) {
            // On a settings value, right-click turns it down; anywhere else it goes back.
            int idx = game.state == State.SETTINGS ? game.listIndexAt(gy) : -1;
            if (game.settings.isValueIndex(idx)) { game.settingsIndex = idx; game.settings.adjustSetting(-1); }
            else goBack();
            return;
        }

        if (game.state == State.LEVEL_SELECT) {
            if (MenuScreens.CHALLENGE_BUTTON.contains(gx, gy)) {
                startSelectedChallenge();
                return;
            }
            int idx = game.planetIndexAt(gx, gy);
            if (idx >= 0) {
                game.levelSelectIndex = idx;
                confirmCurrent();
            }
            return;
        }
        int idx = game.listIndexAt(gy);
        if (idx >= 0) {
            game.setSelectionForState(idx);
            confirmCurrent();
        }
    }

    void confirmCurrent() {
        switch (game.state) {
            case MENU -> {
                game.playSfx(Sfx.CONFIRM);
                String[] actions = game.menuActions();
                game.menuIndex = clampInt(game.menuIndex, 0, actions.length - 1);
                switch (actions[game.menuIndex]) {
                    case "continue" -> game.startGame(game.lastLevel);
                    case "school" -> game.startFlightSchool();
                    case "start" -> game.startGame(1);
                    case "practice" -> { game.studyIndex = 0; game.state = State.STUDY; }
                    case "select" -> { game.levelSelectIndex = slotForDestination(destinationForLevel(game.lastLevel)); game.state = State.LEVEL_SELECT; }
                    case "howto" -> { game.howToPage = 0; game.state = State.HOW_TO_PLAY; }
                    case "records" -> {
                        game.newHighScoreRank = -1;
                        game.highScoresReturnState = State.MENU;
                        game.state = State.HIGH_SCORES;
                    }
                    case "settings" -> { game.settingsMenuIndex = 0; game.state = State.SETTINGS_MENU; }
                    default -> { game.saveFile.saveProgress(); Sfx.shutdown(); System.exit(0); }
                }
            }
            case SETTINGS_MENU -> {
                int sel = game.settingsMenuIndex;
                SettingsScreen.Group[] groups = SettingsScreen.Group.values();
                if (sel >= 0 && sel < groups.length) { game.playSfx(Sfx.CONFIRM); game.settings.open(groups[sel]); }
                else if (sel == SettingsScreen.HUB_CONTROLS) { game.playSfx(Sfx.CONFIRM); game.controlsIndex = 0; game.awaitingKeybind = false; game.state = State.CONTROLS; }
                else if (sel == SettingsScreen.HUB_RESET) {
                    if (!game.resetArmed) { game.resetArmed = true; game.playSfx(Sfx.CLICK); }
                    else { game.resetArmed = false; game.settings.resetEverything(); game.playSfx(Sfx.CONFIRM); }
                }
                else goBack();
            }
            case SETTINGS -> {
                SettingsScreen.Row row = game.settings.currentRow();
                if (row == SettingsScreen.Row.RESET) {
                    if (!game.resetArmed) { game.resetArmed = true; game.playSfx(Sfx.CLICK); }
                    else { game.resetArmed = false; game.settings.resetGroup(game.settingsGroup); game.playSfx(Sfx.CONFIRM); }
                }
                else if (row == SettingsScreen.Row.BACK) goBack();
                else game.settings.adjustSetting(1);
            }
            case CONTROLS -> {
                if (game.controlsIndex == game.controlsRows() - 1) {
                    game.playSfx(Sfx.CONFIRM);
                    game.saveFile.saveProgress();
                    game.state = State.SETTINGS_MENU;
                } else if (game.controlsIndex == game.controlsResetRow()) {
                    if (!game.resetArmed) { game.resetArmed = true; game.playSfx(Sfx.CLICK); }
                    else { game.resetArmed = false; game.settings.resetControlsToDefaults(); game.playSfx(Sfx.CONFIRM); }
                } else {
                    game.awaitingKeybind = true;
                    game.playSfx(Sfx.CLICK);
                }
            }
            case LEVEL_SELECT -> {
                int dest = destinationAtSlot(game.levelSelectIndex);
                if (game.isDestinationLocked(dest)) {
                    game.playSfx(Sfx.CLICK);
                } else {
                    game.playSfx(Sfx.CONFIRM);
                    game.startGame(game.destinationStartLevel(dest));
                }
            }
            case PAUSED -> {
                game.playSfx(Sfx.CONFIRM);
                if (game.pauseIndex == 0) { game.state = State.PLAYING; game.syncMusic(); }
                else game.returnToMenu();
            }
            case GAME_OVER -> {
                game.playSfx(Sfx.CONFIRM);
                List<String> actions = game.gameOverActions();
                game.gameOverIndex = clampInt(game.gameOverIndex, 0, actions.size() - 1);
                switch (actions.get(game.gameOverIndex)) {
                    case "continue" -> game.startGame(game.level);
                    case "restart" -> game.startGame(game.gameStartLevel);
                    case "retry" -> game.startChallenge(game.challengePlanet);
                    case "review" -> { game.reviewIndex = 0; game.state = State.REVIEW; }
                    case "scores" -> { game.highScoresIndex = 0; game.state = State.HIGH_SCORES; }
                    default -> game.returnToMenu();
                }
            }
            case HOW_TO_PLAY -> {
                game.playSfx(Sfx.CONFIRM);
                game.state = State.MENU;
            }
            case STUDY -> {
                switch (game.studyActions()[clampInt(game.studyIndex, 0, game.studyActions().length - 1)]) {
                    case "daily" -> { game.playSfx(Sfx.CONFIRM); game.startDailyFlight(); }
                    case "weak" -> { game.playSfx(Sfx.CONFIRM); game.startPractice(); }
                    case "mistakes" -> {
                        if ((game.notebook.practiceCount() == 0)) {
                            game.playSfx(Sfx.CLICK);
                        } else {
                            game.playSfx(Sfx.CONFIRM);
                            game.startMistakesDrill();
                        }
                    }
                    case "notebook" -> { game.playSfx(Sfx.CONFIRM); game.notebookIndex = 0; game.state = State.NOTEBOOK; }
                    case "topics" -> { game.playSfx(Sfx.CONFIRM); game.topicStatsIndex = 0; game.state = State.TOPIC_STATS; }
                    case "codex" -> { game.playSfx(Sfx.CONFIRM); game.codexIndex = game.rewards.codexOpeningSlot(); game.state = State.CODEX; }
                    case "export" -> game.exportResults();
                    default -> goBack();
                }
            }
            case PILOTS -> game.pilots.confirm();
            case TOPIC_STATS -> {
                if (game.topicStatsIndex >= ALL_TOPICS.size()) {
                    goBack();
                } else {
                    game.playSfx(Sfx.CONFIRM);
                    game.startPractice(ALL_TOPICS.get(game.topicStatsIndex));
                }
            }
            case NOTEBOOK, REVIEW, CODEX -> goBack();
            case STAR -> {
                game.playSfx(Sfx.CONFIRM);
                if (game.starIntro) {
                    // The final test begins.
                    game.starIntro = false;
                    game.state = State.PLAYING;
                    game.nextQuestion();
                } else {
                    // The star is beaten; that is the end of the journey.
                    game.victoryIndex = 0;
                    game.finishRun(State.VICTORY);
                }
            }
            case HIGH_SCORES -> {
                game.playSfx(Sfx.CONFIRM);
                game.newHighScoreRank = -1;
                game.state = game.highScoresReturnState;
            }
            case NAME_ENTRY -> game.submitHighScore();
            case VICTORY -> {
                game.playSfx(Sfx.CONFIRM);
                // "Fly On" carries the player past the star for as long as they like.
                switch (game.victoryIndex) {
                    case 0 -> { game.beyondTheStar = true; game.state = State.PLAYING; game.nextQuestion(); }
                    case 1 -> game.startGame(1);
                    default -> game.returnToMenu();
                }
            }
            default -> { }
        }
    }

    static final double PAD_STEER = 0.35, PAD_NAV = 0.6, PAD_TRIGGER = 0.3;

    static final double PAD_REPEAT_DELAY = 0.4, PAD_REPEAT_RATE = 0.11;

    int padPrevButtons = 0;

    /** The menu direction held last tick, and time until it repeats. */
    int padNavX = 0, padNavY = 0;

    double padRepeatX = 0, padRepeatY = 0;

    void pollGamepad(double dt) {
        applyPad(game.hasFocus() ? Gamepad.poll() : Gamepad.NONE, dt);
    }

    void applyPad(Gamepad.Snapshot s, double dt) {
        if (!s.connected) {
            game.padLeft = game.padRight = game.padBoost = game.padSlow = false;
            padPrevButtons = 0;
            padNavX = padNavY = 0;
            return;
        }
        int pressed = s.buttons & ~padPrevButtons;
        padPrevButtons = s.buttons;
        boolean playing = game.state == State.PLAYING, menu = game.state == State.MENU;

        // Flight, held: the stick or D-pad steers, the triggers work the thrusters.
        boolean wasLeft = game.padLeft, wasRight = game.padRight;
        boolean flying = playing || menu;
        game.padLeft = flying && (s.has(Gamepad.DPAD_LEFT) || s.lx < -PAD_STEER);
        game.padRight = flying && (s.has(Gamepad.DPAD_RIGHT) || s.lx > PAD_STEER);
        game.padBoost = flying && (s.rt > PAD_TRIGGER || (playing && (s.has(Gamepad.DPAD_UP) || s.ly > PAD_NAV)));
        game.padSlow = flying && (s.lt > PAD_TRIGGER || (playing && (s.has(Gamepad.DPAD_DOWN) || s.ly < -PAD_NAV)));
        if (game.padLeft && !wasLeft) game.steerRightLast = false;
        if (game.padRight && !wasRight) game.steerRightLast = true;

        // Waiting for a key to bind: only B does anything, and it cancels.
        if (game.awaitingKeybind) {
            if ((pressed & Gamepad.B) != 0) padPress(KeyEvent.VK_ESCAPE);
            return;
        }

        if ((pressed & Gamepad.START) != 0) padPress(playing || game.state == State.PAUSED ? KeyEvent.VK_ESCAPE : KeyEvent.VK_ENTER);
        if ((pressed & Gamepad.A) != 0) padPress(playing || game.state == State.STAR ? game.keyShoot : KeyEvent.VK_ENTER);
        if ((pressed & Gamepad.X) != 0 && flying) padPress(game.keyShoot);
        if ((pressed & Gamepad.B) != 0) {
            // Never out of a run by accident: B only steps back through menus.
            if (game.state == State.NAME_ENTRY) padPress(KeyEvent.VK_BACK_SPACE);
            else if (!playing) padPress(KeyEvent.VK_ESCAPE);
        }
        if ((pressed & Gamepad.BACK) != 0 && playing) padPress(KeyEvent.VK_TAB);
        if ((pressed & Gamepad.Y) != 0 && game.state == State.LEVEL_SELECT) padPress(KeyEvent.VK_C);
        if ((pressed & Gamepad.Y) != 0 && game.state == State.MENU) padPress(KeyEvent.VK_P);
        if ((pressed & Gamepad.LB) != 0 && !flying) padPress(KeyEvent.VK_LEFT);
        if ((pressed & Gamepad.RB) != 0 && !flying) padPress(KeyEvent.VK_RIGHT);

        // Menus: the D-pad or stick moves the selection, repeating while held.
        if (playing) {
            padNavX = padNavY = 0;
            return;
        }
        int navY = s.has(Gamepad.DPAD_UP) || (!menu && s.ly > PAD_NAV) ? -1
            : s.has(Gamepad.DPAD_DOWN) || (!menu && s.ly < -PAD_NAV) ? 1 : 0;
        int navX = menu ? 0 : s.has(Gamepad.DPAD_LEFT) || s.lx < -PAD_NAV ? -1
            : s.has(Gamepad.DPAD_RIGHT) || s.lx > PAD_NAV ? 1 : 0;

        if (navY != padNavY) {
            padNavY = navY;
            padRepeatY = PAD_REPEAT_DELAY;
            if (navY != 0) padNavigate(navY);
        } else if (navY != 0 && (padRepeatY -= dt) <= 0) {
            padRepeatY = PAD_REPEAT_RATE;
            padNavigate(navY);
        }
        if (navX != padNavX) {
            padNavX = navX;
            padRepeatX = PAD_REPEAT_DELAY;
            if (navX != 0) padPress(navX < 0 ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT);
        } else if (navX != 0 && (padRepeatX -= dt) <= 0) {
            padRepeatX = PAD_REPEAT_RATE;
            padPress(navX < 0 ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT);
        }
    }

    /**
     * Up or down one row. The main menu leaves the arrow keys to flying, so
     * the pad moves its selection directly; everywhere else it presses the
     * matching arrow key.
     */
    void padNavigate(int dir) {
        if (game.state == State.MENU) {
            int n = game.menuItems().length;
            game.menuIndex = ((game.menuIndex + dir) % n + n) % n;
            game.playSfx(Sfx.CLICK);
        } else {
            padPress(dir < 0 ? KeyEvent.VK_UP : KeyEvent.VK_DOWN);
        }
    }

    /** A tap of one key: pressed, then released at once so nothing stays held. */
    void padPress(int keyCode) {
        long now = System.currentTimeMillis();
        keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, now, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
        keyReleased(new KeyEvent(game, KeyEvent.KEY_RELEASED, now, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();

        if (game.state == State.CONTROLS && game.awaitingKeybind) {
            if (k != KeyEvent.VK_ESCAPE && k != KeyEvent.VK_F11 && k != KeyEvent.VK_F3) {
                for (int i = 0; i < game.controlActionNames.length; i++) {
                    if (i != game.controlsIndex && game.settings.getControlKey(i) == k) game.settings.setControlKey(i, game.settings.getControlKey(game.controlsIndex));
                }
                game.settings.setControlKey(game.controlsIndex, k);
                game.playSfx(Sfx.CONFIRM);
                game.saveFile.saveProgress();
            }
            game.awaitingKeybind = false;
            return;
        }

        if (k == KeyEvent.VK_F11) {
            game.toggleFullscreen();
            return;
        }
        if (k == KeyEvent.VK_F3) {
            game.showFps = !game.showFps;
            return;
        }
        // The pilot screen takes every key: it is typed into as well as chosen from.
        if (game.state == State.PILOTS) {
            game.pilots.keyPressed(k, e.getKeyChar());
            return;
        }
        if (game.confirmBlocked() && (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE || k == game.keyShoot)) return;
        if (k == game.keyLeft || k == KeyEvent.VK_LEFT) { game.leftHeld = true; game.steerRightLast = false; }
        if (k == game.keyRight || k == KeyEvent.VK_RIGHT) { game.rightHeld = true; game.steerRightLast = true; }
        if (k == game.keyBoost || k == KeyEvent.VK_UP) game.boostHeld = true;
        if (k == game.keySlow || k == KeyEvent.VK_DOWN) game.slowHeld = true;

        if (game.state == State.PLAYING) {
            // While an explanation is held open, the shoot key means "continue"
            // rather than firing, so the press is not swallowed.
            if (game.bannerAwaitInput && (k == game.keyShoot || k == KeyEvent.VK_ENTER)) {
                game.dismissBanner();
                return;
            }
            if (k == game.keyShoot) game.shoot();
            if (k == KeyEvent.VK_TAB) game.showQuestionPanel = !game.showQuestionPanel;
            if (k == KeyEvent.VK_ESCAPE) {
                game.leftHeld = false; game.rightHeld = false; game.boostHeld = false; game.slowHeld = false; game.throttle = 0; game.rocketVX = 0; game.rocketY = ROCKET_BASE_Y;
                game.pauseIndex = 0;
                game.state = State.PAUSED;
                game.syncMusic();
            }
            return;
        }

        if (game.state == State.PAUSED) {
            navigateList(k, game.pauseItems.length, i -> game.pauseIndex = i, game.pauseIndex);
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.GAME_OVER) {
            navigateList(k, game.gameOverItems().length, i -> game.gameOverIndex = i, game.gameOverIndex);
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.VICTORY) {
            navigateList(k, game.victoryItems.length, i -> game.victoryIndex = i, game.victoryIndex);
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.REVIEW || game.state == State.NOTEBOOK) {
            if (isLeftKey(k) || k == KeyEvent.VK_UP) browse(-1);
            if (isRightKey(k) || k == KeyEvent.VK_DOWN) browse(1);
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER) goBack();
            return;
        }

        if (game.state == State.CODEX) {
            if (isLeftKey(k) || k == KeyEvent.VK_UP) browseCodex(-1);
            if (isRightKey(k) || k == KeyEvent.VK_DOWN) browseCodex(1);
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER) goBack();
            return;
        }

        if (game.state == State.STUDY) {
            navigateList(k, game.studyActions().length, i -> game.studyIndex = i, game.studyIndex);
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.TOPIC_STATS) {
            navigateList(k, game.topicStatsRows(), i -> game.topicStatsIndex = i, game.topicStatsIndex);
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.HOW_TO_PLAY) {
            if (k == game.keyLeft || k == KeyEvent.VK_LEFT) {
                game.howToPage = (game.howToPage - 1 + HOW_TO_PAGES) % HOW_TO_PAGES;
                game.playSfx(Sfx.CLICK);
            } else if (k == game.keyRight || k == KeyEvent.VK_RIGHT) {
                game.howToPage = (game.howToPage + 1) % HOW_TO_PAGES;
                game.playSfx(Sfx.CLICK);
            } else if (k == KeyEvent.VK_F) {
                game.playSfx(Sfx.CONFIRM);
                game.startFlightSchool();
            } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_ESCAPE) {
                confirmCurrent();
            }
            return;
        }

        if (game.state == State.HIGH_SCORES) {
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_ESCAPE) confirmCurrent();
            return;
        }

        if (game.state == State.NAME_ENTRY) {
            handleNameEntry(k, e.getKeyChar());
            return;
        }

        if (game.state == State.STAR) {
            if (!game.starEnding.starFinishedSpeaking()) {
                // Any of the usual "go on" keys hurries the star along.
                if (k == game.keyShoot || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) game.starEnding.advanceStarSpeech();
                return;
            }
            // One way forward either way: into the fight, or out of the journey.
            if (k == game.keyShoot || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) confirmCurrent();
            return;
        }

        if (game.state == State.MENU) {
            // The main menu is chosen with the mouse, so that W/S/A/D stay free
            // to fly the rocket without dragging the selection around with them.
            // Number keys give a keyboard-only player a way in without stealing
            // any of the flight controls.
            if (k == KeyEvent.VK_P && k != game.keyLeft && k != game.keyRight && k != game.keyShoot
                    && k != game.keyBoost && k != game.keySlow) {
                game.playSfx(Sfx.CONFIRM);
                game.pilots.open();
                return;
            }
            if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) {
                int pick = k - KeyEvent.VK_1;
                if (pick < game.menuItems().length) {
                    game.menuIndex = pick;
                    confirmCurrent();
                }
                return;
            }
            if (k == game.keyShoot) game.menuShoot();
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            return;
        }

        if (game.state == State.LEVEL_SELECT) {
            if (isLeftKey(k) || k == KeyEvent.VK_A || k == KeyEvent.VK_W || k == KeyEvent.VK_UP) {
                game.levelSelectIndex = (game.levelSelectIndex - 1 + game.destinationCount()) % game.destinationCount();
                game.playSfx(Sfx.CLICK);
            } else if (isRightKey(k) || k == KeyEvent.VK_D || k == KeyEvent.VK_S || k == KeyEvent.VK_DOWN) {
                game.levelSelectIndex = (game.levelSelectIndex + 1) % game.destinationCount();
                game.playSfx(Sfx.CLICK);
            }
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_C) startSelectedChallenge();
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.SETTINGS_MENU) {
            navigateList(k, SettingsScreen.HUB_ROWS, i -> game.settingsMenuIndex = i, game.settingsMenuIndex);
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.SETTINGS) {
            navigateList(k, game.settings.rows().length, i -> game.settingsIndex = i, game.settingsIndex);
            boolean nav = k == KeyEvent.VK_W || k == KeyEvent.VK_S;   // a steer key rebound onto W/S still only navigates
            if (game.settings.isValueIndex(game.settingsIndex) && !nav && (isLeftKey(k) || isRightKey(k))) {
                game.settings.adjustSetting(isLeftKey(k) ? -1 : 1);
            } else if (k == KeyEvent.VK_ENTER) {
                confirmCurrent();
            }
            if (k == KeyEvent.VK_ESCAPE) goBack();
            return;
        }

        if (game.state == State.CONTROLS) {
            navigateList(k, game.controlsRows(), i -> game.controlsIndex = i, game.controlsIndex);
            if (k == KeyEvent.VK_ENTER) confirmCurrent();
            if (k == KeyEvent.VK_ESCAPE) goBack();
        }
    }

    void handleNameEntry(int k, char typed) {
        if (k == KeyEvent.VK_ENTER) {
            game.submitHighScore();
            return;
        }
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
            game.nameEntry[game.nameEntryIndex] = cycleLetter(game.nameEntry[game.nameEntryIndex], 1);
            game.playSfx(Sfx.CLICK);
        } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            game.nameEntry[game.nameEntryIndex] = cycleLetter(game.nameEntry[game.nameEntryIndex], -1);
            game.playSfx(Sfx.CLICK);
        } else if (k == KeyEvent.VK_LEFT) {
            game.nameEntryIndex = (game.nameEntryIndex - 1 + HighScores.NAME_LENGTH) % HighScores.NAME_LENGTH;
            game.playSfx(Sfx.CLICK);
        } else if (k == KeyEvent.VK_RIGHT) {
            game.nameEntryIndex = (game.nameEntryIndex + 1) % HighScores.NAME_LENGTH;
            game.playSfx(Sfx.CLICK);
        } else if (k == KeyEvent.VK_BACK_SPACE) {
            game.nameEntryIndex = Math.max(0, game.nameEntryIndex - 1);
            game.playSfx(Sfx.CLICK);
        } else if (Character.isLetter(typed)) {
            game.nameEntry[game.nameEntryIndex] = Character.toUpperCase(typed);
            game.playSfx(Sfx.CLICK);
            if (game.nameEntryIndex < HighScores.NAME_LENGTH - 1) game.nameEntryIndex++;
        }
    }

    static char cycleLetter(char c, int dir) {
        int i = c - 'A';
        i = ((i + dir) % 26 + 26) % 26;
        return (char) ('A' + i);
    }

    boolean isLeftKey(int k) { return k == game.keyLeft || k == KeyEvent.VK_LEFT; }

    boolean isRightKey(int k) { return k == game.keyRight || k == KeyEvent.VK_RIGHT; }

    void navigateList(int k, int count, java.util.function.IntConsumer setter, int current) {
        if (k == KeyEvent.VK_W || k == KeyEvent.VK_UP) {
            setter.accept((current - 1 + count) % count);
            game.resetArmed = false;
            game.playSfx(Sfx.CLICK);
        } else if (k == KeyEvent.VK_S || k == KeyEvent.VK_DOWN) {
            setter.accept((current + 1) % count);
            game.resetArmed = false;
            game.playSfx(Sfx.CLICK);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == game.keyLeft || k == KeyEvent.VK_LEFT) game.leftHeld = false;
        if (k == game.keyRight || k == KeyEvent.VK_RIGHT) game.rightHeld = false;
        if (k == game.keyBoost || k == KeyEvent.VK_UP) game.boostHeld = false;
        if (k == game.keySlow || k == KeyEvent.VK_DOWN) game.slowHeld = false;
    }

    @Override
    public void keyTyped(KeyEvent e) { }
}
