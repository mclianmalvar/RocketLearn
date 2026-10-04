package rocketlearn;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public final class RocketLearn extends JPanel implements ActionListener {

    // Parts of the game that live in their own files; each keeps a reference back here.
    final SaveFile saveFile = new SaveFile(this);
    final ShipPainter ship = new ShipPainter(this);
    final SettingsScreen settings = new SettingsScreen(this);
    final StarEnding starEnding = new StarEnding(this);
    final Scenery scenery = new Scenery(this);
    final InputHandler input = new InputHandler(this);
    final Hud hud = new Hud(this);
    final PlayfieldPainter playfield = new PlayfieldPainter(this);
    final MenuScreens menus = new MenuScreens(this);
    final StudyScreens study = new StudyScreens(this);
    final RecordsScreens records = new RecordsScreens(this);
    final Rewards rewards = new Rewards(this);
    final QuestionDealer dealer = new QuestionDealer(this);
    final PilotScreen pilots = new PilotScreen(this);

    static final long serialVersionUID = 1L;

    /**
     * The game's version: shown on the main menu and in the teacher's export,
     * and read by package.bat for the jar and RocketLearn.exe. Change it here
     * only - major.minor.patch, numbers only, as jpackage requires.
     */
    static final String VERSION = "1.1.0";

    static final int GAME_WIDTH = 900, GAME_HEIGHT = 506;
    static final int CHIP_W = 140, CHIP_H = 56;

    static final List<Planet> planets = Planet.ALL;


    double rocketX = GAME_WIDTH / 2.0;
    static final double ROCKET_BASE_Y = GAME_HEIGHT - 90;
    static final double ROCKET_TOP_Y = ROCKET_BASE_Y - 70;
    static final double ROCKET_BOTTOM_Y = ROCKET_BASE_Y + 15;
    double rocketY = ROCKET_BASE_Y;
    final int rocketW = 40, rocketH = 60;
    static final double ROCKET_MAX_SPEED = 380;
    static final double THROTTLE_RAMP_UP = 0.35, THROTTLE_RAMP_DOWN = 1.0;
    double throttle = 0;
    static final double ASTEROID_BOOST_MULT = 3.0, ASTEROID_SLOW_MULT = 1.0 / 3.0;
    double asteroidSpeedMultiplier() {
        double base = throttle >= 0 ? 1.0 + (ASTEROID_BOOST_MULT - 1.0) * throttle : 1.0 + (1.0 - ASTEROID_SLOW_MULT) * throttle;
        // A solar flare drives everything down harder while it burns.
        return base * currentPlanet().fallScale * (starFlaring() ? 1.3 : 1.0);
    }
    double rocketMaxSpeed() { return fastTimer ? ROCKET_MAX_SPEED * 1.15 : ROCKET_MAX_SPEED; }
    double rocketVX = 0;
    boolean leftHeld, rightHeld, boostHeld, slowHeld;
    /** Which side was pressed most recently, so holding both steers that way. */
    boolean steerRightLast;
    /** Pushing against the current drift bites this much harder, so turning around is quick. */
    static final double COUNTER_STEER = 2.2;

    /** The same four flight inputs as held by a controller, read fresh every tick. */
    boolean padLeft, padRight, padBoost, padSlow;

    boolean leftInput() { return leftHeld || padLeft; }
    boolean rightInput() { return rightHeld || padRight; }
    boolean boostInput() { return boostHeld || padBoost; }
    boolean slowInput() { return slowHeld || padSlow; }

    /** -1 slow, 0 cruise, 1 boost: where the throttle is heading. Both held cancel out. */
    double throttleTarget() {
        boolean boost = boostInput(), slow = slowInput();
        return boost && !slow ? 1.0 : slow && !boost ? -1.0 : 0.0;
    }

    /** -1, 0 or 1. With both sides held the newer press wins, rather than the two cancelling. */
    int steerDir() {
        boolean left = leftInput(), right = rightInput();
        if (left && right) return steerRightLast ? 1 : -1;
        return left ? -1 : right ? 1 : 0;
    }

    /** One frame of sideways control: accelerate toward the held side, or coast down. */
    double steer(double vx, double accel, double friction, double dt) {
        int dir = steerDir();
        if (dir == 0) return vx * Math.pow(friction, dt * 60);
        double a = Math.signum(vx) == -dir ? accel * COUNTER_STEER : accel;
        return vx + dir * a * dt;
    }
    boolean exploding = false;
    boolean rocketAlive = true;
    double explodeTimer = 0;
    double flameFlicker = 0;
    double fireCooldown = 0;
    double shakeTime = 0, shakeMag = 0;
    double shakeX = 0, shakeY = 0;
    boolean timeExpired = false;
    boolean showQuestionPanel = true;

    List<Bullet> bullets = new ArrayList<>();
    List<AnswerOption> options = new ArrayList<>();
    List<PowerUp> powerUps = new ArrayList<>();
    List<Particle> particles = new ArrayList<>();
    List<double[]> stars = new ArrayList<>();
    List<double[]> menuAsteroids = new ArrayList<>();

    static final double MENU_ROCKET_BASE_Y = GAME_HEIGHT - 132;
    static final double MENU_ROCKET_TOP_Y = MENU_ROCKET_BASE_Y - 74;
    static final double MENU_ROCKET_BOTTOM_Y = MENU_ROCKET_BASE_Y + 22;
    static final double MENU_ROCKET_SCALE = 1.2;
    static final double MENU_SPEED_SCALE = 0.65;
    /** Where the menu rocket rests: the left margin, clear of the stats boxes and the Starborn halo's reach. */
    static final double MENU_ROCKET_START_X = 84;
    /** Baseline of the ROCKET LEARN title; the tagline and list sit below it. */
    static final int MENU_TITLE_Y = 66;
    double menuRocketX = MENU_ROCKET_START_X;
    double menuRocketY = MENU_ROCKET_BASE_Y;
    double menuRocketVX = 0;

    int hearts = 3;
    int score = 0;
    int level = 1;
    Question currentQuestion;
    String questionText = "";
    boolean repeatQuestion = false;
    int missCount = 0;
    static final int HINT_AFTER_MISSES = 2;

    /** True while the right asteroid is lit up for a player who has missed this question enough times. */
    boolean hintActive() { return missCount >= HINT_AFTER_MISSES; }

    ExplainMode explainMode = ExplainMode.HINT;

    boolean explanationsOn() { return practiceMode || explainMode != ExplainMode.OFF; }

    double timeLeft, maxTime;

    String bannerText = "";
    String bannerSubtitle = null;
    Color bannerColor = Color.WHITE;
    double bannerTimer = 0;
    double bannerDuration = 0;
    boolean bannerEmphasis = false;
    boolean bannerHidePanel = false;
    /** When true the banner holds indefinitely until the player presses to continue. */
    boolean bannerAwaitInput = false;
    /** True while the banner is explaining a wrong answer, so the music can make room for it. */
    boolean bannerExplains = false;
    BannerAction pendingAction = BannerAction.NONE;

    int correctStreak = 0;
    static final int HEAL_STREAK = 5;

    double windPhase = 0;
    double dustTimer = 0;
    List<Debris> debris = new ArrayList<>();

    boolean hasShield = false;
    double freezeTimer = 0;
    static final double FREEZE_DURATION = 4.0;
    static final double POWERUP_CHANCE = 0.3;
    static final double POWERUP_W = 54, POWERUP_H = 54;
    double powerUpSpawnTimer = -1;

    int highScore = 0;
    List<Question> missedQuestions = new ArrayList<>();
    Map<String, Integer> topicMisses = new LinkedHashMap<>();
    int reviewIndex = 0;
    boolean soundOn = true;

    /**
     * The background music, switched separately from the rest of the audio.
     * Sound is the master: with it off nothing plays at all, and this only
     * decides whether the music joins the effects when it is on.
     */
    boolean musicOn = true;

    /**
     * Loudness per channel, 0 to 100 in steps of VOLUME_STEP. Master scales the
     * other two rather than standing beside them, so what a line finally plays
     * at is master x its own level.
     */
    int masterVolume = 100, musicVolume = 100, effectsVolume = 100;
    static final int VOLUME_STEP = 10;

    double musicLevel() { return masterVolume / 100.0 * (musicVolume / 100.0); }
    double effectsLevel() { return masterVolume / 100.0 * (effectsVolume / 100.0); }

    /** Pushes the current levels down to the audio lines. */
    void syncVolumes() {
        Sfx.setMusicGain(musicLevel());
        Sfx.setEffectsGain(effectsLevel());
    }

    double windLean = 0;

    HighScores highScores = new HighScores();
    char[] nameEntry = { 'A', 'A', 'A' };
    int nameEntryIndex = 0;
    int newHighScoreRank = -1;

    int howToPage = 0;
    static final int HOW_TO_PAGES = 3;
    int highScoresIndex = 0;
    State highScoresReturnState = State.GAME_OVER;

    static final double FADE_TIME = 0.28;
    double fadeTimer = 0;
    State fadeFromState = null;

    // ---- effects -------------------------------------------------------
    /** Briefly suspends the simulation on impact so hits land with weight. */
    double hitStopTimer = 0;
    double muzzleFlash = 0;
    double recoil = 0;
    double damageFlash = 0;
    double explosionFlash = 0;
    double shieldFlash = 0;
    double scorePop = 0;
    double displayScore = 0;
    int lostHeartIndex = -1;
    double heartPunch = 0;
    boolean warping = false;
    int lastTickSecond = -1;
    List<HitFlash> hitFlashes = new ArrayList<>();

    // ---- the star's ending ---------------------------------------------
    /** How many of the star's lines have been revealed, and how far into the current one. */
    int starLineIndex = 0;
    double starReveal = 0;
    /** True once the player told the star they wanted to keep learning. */
    boolean beyondTheStar = false;

    /** True while the star is introducing the final test rather than closing it. */
    boolean starIntro = false;

    // ---- progression and modes ----------------------------------------
    Stats stats = new Stats();
    Notebook notebook = new Notebook();
    boolean practiceMode = false;
    String practiceFocus = null;
    /** A practice run that asks only the notebook's uncleared questions, and ends once they are all cleared. */
    boolean mistakesDrill = false;
    /** A daily flight: today's ten questions, no hearts lost, scored and counted towards the streak. */
    boolean dailyRun = false;
    final DailyFlight daily = new DailyFlight();
    /** Flight School, the guided first flight; done once flown, and saved. */
    boolean flightSchoolRun = false, flightSchoolDone = false;
    final FlightSchool flightSchool = new FlightSchool(this);
    int shipSkinIndex = 0;
    /** The chosen engine trail, an index into EngineTrail.ALL. */
    int trailIndex = 0;
    /** Medals and finished planets, and the planet this run is being judged on. */
    PlanetRecords planetRecords = new PlanetRecords();

    // ---- the study hub -------------------------------------------------
    int studyIndex = 0;
    int notebookIndex = 0;
    int topicStatsIndex = 0;
    /** The codex card being shown, as a stop along the route (0 = the first planet). */
    int codexIndex = 0;

    /** Every topic in the bank, in the order the bank first uses them. */
    static final List<String> ALL_TOPICS = new ArrayList<>();
    static {
        Set<String> seen = new LinkedHashSet<>();
        for (List<Question> tier : QuestionBank.TIERS) for (Question q : tier) seen.add(q.topic);
        ALL_TOPICS.addAll(seen);
    }

    String[] studyActions() {
        return new String[]{ "daily", "weak", "mistakes", "notebook", "topics", "codex", "export", "back" };
    }

    /** What the last export did - where the file went, or why it failed - until the player moves on. */
    String exportNotice = null;

    /** Writes the results spreadsheet and says where it went. */
    void exportResults() {
        try {
            java.io.File out = ResultsExport.export(stats, notebook, planetRecords, highestLevelReached,
                ResultsExport.studentName(), java.time.LocalDateTime.now());
            exportNotice = "Saved: " + out.getName() + "  (in " + out.getAbsoluteFile().getParent() + ")";
            playSfx(Sfx.CONFIRM);
        } catch (java.io.IOException | SecurityException e) {
            exportNotice = "Could not save the results: " + e.getMessage();
            playSfx(Sfx.CLICK);
        }
    }

    String[] studyItems() {
        String weak = stats.weakestTopic();
        int toClear = notebook.toClearCount(), toReview = notebook.dueCount();
        String mistakes = toClear == 0 && toReview == 0 ? "Practice My Mistakes (none yet)"
            : toReview == 0 ? "Practice My Mistakes (" + toClear + " to clear)"
            : toClear == 0 ? "Practice My Mistakes (" + toReview + " to review)"
            : "Practice My Mistakes (" + toClear + " to clear, " + toReview + " to review)";
        int streakNow = daily.currentStreak();
        String dailyLabel = daily.doneToday() ? "Daily Flight (today: " + daily.lastScore + "/" + DailyFlight.QUESTIONS + ", streak " + streakNow + ")"
            : streakNow > 0 ? "Daily Flight (keep your " + streakNow + "-day streak)" : "Daily Flight (new today)";
        return new String[]{
            dailyLabel,
            weak == null ? "Practice - Mixed Topics" : "Practice - " + weak,
            mistakes,
            "Notebook (" + notebook.size() + ")",
            "Topic Stats",
            "Planet Codex (" + planetRecords.finishedCount(highestLevelReached) + " of " + planets.size() + ")",
            "Export Results for Teacher",
            "Back"
        };
    }

    String studyHint(int idx) {
        return switch (studyActions()[clampInt(idx, 0, studyActions().length - 1)]) {
            case "daily" -> daily.doneToday()
                ? "Flown today - fly again for practice, or come back tomorrow for a new ten"
                : "Ten questions, the same for everyone today - fly one every day to build a streak";
            case "weak" -> practiceFocusHint();
            case "mistakes" -> notebook.toClearCount() > 0
                ? "Only questions you've missed - answer each right first try to clear it"
                : notebook.dueCount() > 0
                ? "Cleared questions come back after 1, 3 and 7 days - recall them to make them stick"
                : "Questions you miss land here, to drill until they stick";
            case "notebook" -> notebook.isEmpty()
                ? "Every explanation you've needed will be kept here to reread"
                : "Reread the explanation for every question you've missed";
            case "topics" -> "Your accuracy in every topic - pick one to practice it";
            case "codex" -> "A card for every planet you finish: its facts, its topics and your medal";
            case "export" -> exportNotice != null ? exportNotice
                : "Saves a spreadsheet of your topic scores and missed questions to hand in";
            default -> "Back to the main menu";
        };
    }

    /** How many questions later a missed question comes back for another go. */
    static final int REVIEW_GAP = 5;

    void playSfx(byte[] pcm) { if (soundOn) Sfx.play(pcm); }

    /** Which track belongs to wherever the player currently is. */
    int musicTrackForState() {
        boolean inPlay = state == State.PLAYING || state == State.PAUSED || state == State.STAR;
        if (!inPlay) return Sfx.MENU_TRACK;
        return atStar() ? Sfx.BOSS_TRACK : difficultySector();
    }

    /**
     * Keeps the background track matched to where the player is: the star has
     * its own theme, each difficulty sector has its own key, and the menu has
     * the calm one. Safe to call every level - Sfx ignores a repeat request for
     * the track already playing.
     */
    void syncMusic() {
        if (!soundOn || !musicOn) {
            Sfx.stopMusic();
            return;
        }
        // A paused game holds its track rather than switching away from it -
        // startMusic picks the same one back up where it left off on resume.
        if (state == State.PAUSED) {
            Sfx.pauseMusic();
            return;
        }
        Sfx.startMusic(musicTrackForState());
    }

    /**
     * A fixed seed for the game's own randomness, or 0 to let every run differ.
     *
     * Only the headless tools set this, and it matters most to BalanceCheck.
     * That report judges a tuning change by how the numbers move, which is only
     * readable when both runs saw the same questions, the same spawn heights and
     * the same lane swaps. Left unseeded, destinations that the change could not
     * possibly have touched still moved by ten points between runs, which is
     * wider than most of the differences being looked for.
     */
    static long rngSeed = 0;

    Random rng = rngSeed == 0 ? new Random() : new Random(rngSeed);
    State state = State.MENU;

    int lastLevel = 1;
    int menuIndex = 0;

    boolean hasContinue() { return lastLevel > 1; }

    String[] menuActions() {
        List<String> a = new ArrayList<>();
        // A new player is offered the guided first flight, until it has been flown.
        if (!flightSchoolDone && !hasContinue()) a.add("school");
        if (hasContinue()) a.add("continue");
        a.add("start");
        a.add("practice");
        a.add("select");
        a.add("howto");
        a.add("records");
        a.add("settings");
        a.add("quit");
        return a.toArray(String[]::new);
    }

    String[] menuItems() {
        String[] actions = menuActions();
        String[] labels = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            switch (actions[i]) {
                case "continue":
                    labels[i] = "Continue - Level " + lastLevel + " (" + planets.get(planetIndexForLevel(lastLevel)).name + ")";
                    break;
                case "start": labels[i] = "New Game"; break;
                case "practice": labels[i] = "Practice"; break;
                case "select": labels[i] = "Select Level"; break;
                case "school": labels[i] = "Flight School (start here)"; break;
                case "howto": labels[i] = "How to Play"; break;
                case "records": labels[i] = "Records"; break;
                case "settings": labels[i] = "Settings"; break;
                default: labels[i] = "Quit"; break;
            }
        }
        return labels;
    }

    /**
     * Records how far the player has got. Practice and the mistakes drill
     * record nothing: with no hearts at stake, flying through a planet there
     * must not unlock the next one, nor the rewards that come with finishing.
     */
    void rememberProgress() {
        // A medal challenge replays a planet already passed: it must not move the Continue point back to it.
        if (practiceMode || inChallenge()) return;
        lastLevel = clampInt(level, 1, STAR_END_LEVEL);
        if (level > highestLevelReached) highestLevelReached = clampInt(level, 1, STAR_END_LEVEL);
    }

    int levelSelectIndex = 0;

    String[] pauseItems = { "Resume", "Main Menu" };
    int pauseIndex = 0;

    int settingsIndex = 0;
    static final int MIN_HEARTS = 3, MAX_HEARTS = 5, HEART_OPTIONS = MAX_HEARTS - MIN_HEARTS + 1;
    int startingHearts = MIN_HEARTS;
    boolean fastTimer = false;

    int keyLeft = KeyEvent.VK_A;
    int keyRight = KeyEvent.VK_D;
    int keyShoot = KeyEvent.VK_SPACE;
    int keyBoost = KeyEvent.VK_W;
    int keySlow = KeyEvent.VK_S;
    String[] controlActionNames = { "Move Left", "Move Right", "Shoot", "Boost", "Slow" };
    int controlsIndex = 0;
    boolean awaitingKeybind = false;

    /** The row selected on the settings hub. */
    int settingsMenuIndex = 0;
    /** Which group's page the Settings screen shows; settingsIndex is the row on it. */
    SettingsScreen.Group settingsGroup = SettingsScreen.Group.GAMEPLAY;

    /** Calms the screen for players sensitive to motion: no shake, and the full-screen flashes turned well down. */
    boolean reduceMotion = false;
    /** Bigger question, answer and explanation text, for small screens, projectors and anyone who wants it. */
    boolean largeText = false;
    /** Solid panels behind questions and answers, and bolder asteroid outlines, for players who need more contrast. */
    boolean highContrast = false;
    /** Adaptive Help: a little more time, and slower rocks, where the player is struggling. */
    boolean adaptiveHelp = true;
    /** How much the current question is eased (1 = not at all), and why, for the HUD. */
    double assist = 1.0;
    String assistReason = null;
    static final double WEAK_TOPIC_ASSIST = 1.25, LAST_HEART_ASSIST = 1.15;
    static final int WEAK_TOPIC_SAMPLES = 5, WEAK_TOPIC_PERCENT = 50;
    /** Read Aloud: off, the question, or the question and its answers. */
    static final int READ_OFF = 0, READ_QUESTIONS = 1, READ_ALL = 2;
    static final String[] READ_LABELS = { "Off", "Questions", "Questions & Answers" };
    int readAloud = READ_OFF;

    /** How strong a full-screen flash should be: full, or a quarter with Reduce Motion on. */
    double flashScale() { return reduceMotion ? 0.25 : 1.0; }

    /** A reading font size, a fifth larger with Large Text on. */
    int textSize(int normal) { return largeText ? Math.round(normal * 1.2f) : normal; }

    /**
     * How much is drawn each frame; lower levels keep slower machines smooth.
     * A new player starts on the level this computer suits; a saved choice
     * replaces it when the save is loaded.
     */
    GraphicsQuality graphics = GraphicsQuality.recommended();

    /** F3 shows frames per second and draw time, so the effect of a graphics level can be seen. */
    boolean showFps = false;
    /** When each of the last second's frames was painted, oldest first. */
    final ArrayDeque<Long> paintTimes = new ArrayDeque<>();
    /** How long the last frame took to paint, in milliseconds. */
    double lastPaintMs = 0;

    /** Frames painted over the last second. */
    int framesPerSecond() { return paintTimes.size(); }

    /** Where a frame is drawn when it is drawn smaller than the screen and stretched; reused while the size holds. */
    java.awt.image.BufferedImage frameBuffer;

    /** Drops the oldest particles past what the graphics setting keeps alive. */
    void trimParticles() {
        int extra = particles.size() - graphics.maxParticles;
        if (extra > 0) particles.subList(0, extra).clear();
    }

    int controlsResetRow() { return controlActionNames.length; }
    int controlsRows() { return controlActionNames.length + 2; }

    /**
     * A reset takes two presses: the first arms it and relabels the row, the
     * second carries it out. Moving the selection or leaving the screen
     * disarms it, so a stray press can never wipe settings on its own.
     */
    boolean resetArmed = false;

    /** Steps and paints the game at the chosen frame rate; stopped by the tests so they can step by hand. */
    final GameLoop timer = new GameLoop(this);
    long lastNanoTime;

    /** A frame rate with no cap: the loop starts the next frame as soon as the last one is shown. */
    static final int FRAME_RATE_UNLIMITED = -1;

    /** The frame rates the setting steps through. 0 means whatever the monitor refreshes at. */
    static final int[] FRAME_RATES = { 30, 60, 120, 144, 240, 0, FRAME_RATE_UNLIMITED };

    /** Frames per second to aim for, 0 to match the monitor, or FRAME_RATE_UNLIMITED for no cap. */
    int frameRate = recommendedFrameRate();

    /**
     * A new player's frame rate: the monitor's own on a computer that suits
     * High graphics, 60 otherwise. More frames are smoother but keep the
     * processor busier, which a slow machine and a laptop battery feel.
     */
    static int recommendedFrameRate() {
        return GraphicsQuality.recommended() == GraphicsQuality.HIGH ? 0 : 60;
    }

    /** The monitor's refresh rate, re-read at most once a second; 60 when the system will not say. */
    int monitorRefreshRate() {
        long now = System.nanoTime();
        if (now - monitorRateCheckedAt > 1_000_000_000L || cachedMonitorRate <= 0) {
            monitorRateCheckedAt = now;
            int hz = 0;
            try {
                GraphicsConfiguration gc = getGraphicsConfiguration();
                GraphicsDevice device = gc != null ? gc.getDevice()
                    : GraphicsEnvironment.isHeadless() ? null : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                if (device != null) hz = device.getDisplayMode().getRefreshRate();
            } catch (RuntimeException ignored) {
                // No display to ask: fall through to 60.
            }
            cachedMonitorRate = hz >= 24 ? hz : 60;
        }
        return cachedMonitorRate;
    }
    private int cachedMonitorRate = 0;
    private long monitorRateCheckedAt = 0;

    /** The frames per second the GameLoop aims for right now, or 0 for as many as it can. */
    int targetFrameRate() {
        return frameRate > 0 ? frameRate : frameRate == 0 ? monitorRefreshRate() : 0;
    }

    /**
     * True when the screen itself paces the frames. In fullscreen, showing a
     * frame waits for the monitor's next refresh, so a loop that also waited
     * on its own clock kept landing just after a refresh and missing it -
     * measured at 62 frames a second when aiming for 120. Then the loop
     * leaves the pacing to the screen.
     */
    boolean screenPacesFrames() {
        if (frame == null || !fullscreen || targetFrameRate() < monitorRefreshRate()) return false;
        try {
            return frame.getGraphicsConfiguration().getDevice().getFullScreenWindow() == frame;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** How the frame rate reads in Settings. */
    String frameRateLabel() {
        return frameRate > 0 ? String.valueOf(frameRate)
            : frameRate == 0 ? "Monitor (" + monitorRefreshRate() + ")" : "Unlimited";
    }

    /** The next frame rate up or down the list, wrapping round. */
    void stepFrameRate(int dir) {
        int i = 0;
        for (int k = 0; k < FRAME_RATES.length; k++) if (FRAME_RATES[k] == frameRate) i = k;
        frameRate = FRAME_RATES[((i + dir) % FRAME_RATES.length + FRAME_RATES.length) % FRAME_RATES.length];
    }

    JFrame frame;
    boolean fullscreen = false;

    void setFrame(JFrame frame) {
        this.frame = frame;
    }

    void toggleFullscreen() {
        if (frame == null) return;
        fullscreen = !fullscreen;
        applyFullscreen(frame, fullscreen, vsync);
        requestFocusInWindow();
    }

    /**
     * Whether fullscreen waits for the monitor's refresh before showing each
     * frame. On, it uses the screen's own fullscreen mode, where every frame is
     * held for the next refresh: no tearing, but never more frames than the
     * monitor shows. Off, it uses a borderless window the size of the screen,
     * which shows frames as soon as they are drawn, so the higher frame rates
     * and Unlimited can run past the refresh rate.
     */
    boolean vsync = true;

    /** Flips V-Sync, rebuilding the fullscreen window so the change takes hold at once. */
    void setVsync(boolean on) {
        vsync = on;
        if (frame != null && fullscreen) {
            applyFullscreen(frame, true, vsync);
            requestFocusInWindow();
        }
    }

    /**
     * Shows the frame fullscreen or as a normal window. With V-Sync on,
     * fullscreen goes through the screen's own fullscreen mode rather than a
     * maximized borderless window: on Windows a maximized one can sit under
     * the taskbar, which hid the bottom of every screen. With it off, the
     * window is set to the screen's full bounds instead, which covers the
     * taskbar the same way without the refresh wait.
     */
    static void applyFullscreen(JFrame frame, boolean on, boolean exclusive) {
        GraphicsDevice screen = frame.getGraphicsConfiguration().getDevice();
        // The screen's size is read before leaving fullscreen mode. Straight
        // after leaving it, Windows reports the size in raw pixels for a
        // moment, ignoring display scaling: on a 125% screen a borderless
        // window came out a quarter too big, with the game's edges off-screen.
        Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();
        // Leave fullscreen mode before rebuilding the window, then drop the old
        // peer so the border can change.
        if (screen.getFullScreenWindow() == frame) screen.setFullScreenWindow(null);
        if (frame.isDisplayable()) frame.dispose();
        frame.setUndecorated(on);
        if (on) {
            frame.setResizable(true);
            if (exclusive && screen.isFullScreenSupported()) {
                screen.setFullScreenWindow(frame);
            } else {
                frame.setBounds(screenBounds);
                frame.setVisible(true);
            }
        } else {
            frame.setExtendedState(JFrame.NORMAL);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }

    RocketLearn() {
        saveFile.loadProgress();
        Runtime.getRuntime().addShutdownHook(new Thread(saveFile::saveProgress));
        setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(input);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                input.onMouseClicked(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) { settings.endSliderDrag(); }

            @Override
            public void mouseExited(MouseEvent e) { mouseGX = -1; mouseGY = -1; }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                leftHeld = false;
                rightHeld = false;
                boostHeld = false;
                slowHeld = false;
                throttle = 0;
                rocketVX = 0;
            }
        });
        double[] depths = { 0.3, 0.62, 1.0 };
        for (int i = 0; i < 140; i++) {
            double depth = depths[i % depths.length];
            stars.add(new double[]{ rng.nextDouble() * GAME_WIDTH, rng.nextDouble() * GAME_HEIGHT, 0.6 + depth * 1.7, depth });
        }
        for (int i = 0; i < 6; i++) {
            menuAsteroids.add(new double[]{
                rng.nextDouble() * GAME_WIDTH, rng.nextDouble() * GAME_HEIGHT,
                10 + rng.nextDouble() * 14,
                (rng.nextBoolean() ? 1 : -1) * (6 + rng.nextDouble() * 8),
                (rng.nextBoolean() ? 1 : -1) * (3 + rng.nextDouble() * 5)
            });
        }
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { input.onMouseMoved(e); }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseGX = (e.getX() - viewOffX) / viewScale;
                mouseGY = (e.getY() - viewOffY) / viewScale;
                if (settings.draggingSlider >= 0) settings.setVolumeRow(settings.draggingSlider, SettingsScreen.sliderValueAt(SettingsScreen.clampSliderX((e.getX() - viewOffX) / viewScale)));
            }
        });
        addMouseWheelListener(e -> input.onMouseWheel(e.getPreciseWheelRotation()));
        lastNanoTime = System.nanoTime();
        timer.start();
        syncVolumes();   // before syncMusic, so the first track opens at the saved level
        Sfx.checkMusicFiles();
        syncMusic();
        if (askWhoIsPlaying) {
            askWhoIsPlaying = false;
            pilots.open();
        }
    }

    static int clampInt(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    static Color lerpColor(Color a, Color b, float t) {
        t = (float) clamp(t, 0, 1);
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t)
        );
    }

    static final Map<Integer, Font> FONT_CACHE = new HashMap<>();
    static Font font(int style, int size) {
        return FONT_CACHE.computeIfAbsent(style * 1000 + size, k -> new Font("SansSerif", style, size));
    }

    /** Code snippets are drawn in this, so indentation and symbols line up. */
    static final Map<Integer, Font> MONO_CACHE = new HashMap<>();
    static Font monoFont(int style, int size) {
        return MONO_CACHE.computeIfAbsent(style * 1000 + size, k -> new Font(Font.MONOSPACED, style, size));
    }

    /** Code sizes tried largest first, until the widest line fits the panel. */
    static final int[] CODE_FONT_SIZES = { 17, 16, 15, 14, 13, 12 };
    /** With Large Text on, bigger sizes are tried first; the normal ones still catch a very long line. */
    static final int[] CODE_FONT_SIZES_LARGE = { 21, 20, 19, 18, 17, 16, 15, 14, 13, 12 };

    /**
     * Picks the largest code size whose widest line still fits, so a long line
     * shrinks the block rather than spilling out of the panel.
     */
    int codeFontSize(Graphics2D g2, String[] lines, int maxWidth) {
        for (int size : largeText ? CODE_FONT_SIZES_LARGE : CODE_FONT_SIZES) {
            FontMetrics fm = g2.getFontMetrics(monoFont(Font.PLAIN, size));
            int widest = 0;
            for (String ln : lines) widest = Math.max(widest, fm.stringWidth(ln));
            if (widest <= maxWidth) return size;
        }
        return CODE_FONT_SIZES[CODE_FONT_SIZES.length - 1];
    }

    /**
     * Draws a snippet as a left-aligned block on its own panel, centred as a
     * whole. Left-aligned because centring each line would throw the
     * indentation away, which is half of what makes code readable.
     */
    int drawCodeBlock(Graphics2D g2, String[] lines, int topY, int maxWidth) {
        int size = codeFontSize(g2, lines, maxWidth);
        g2.setFont(monoFont(Font.PLAIN, size));
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getHeight();
        int widest = 0;
        for (String ln : lines) widest = Math.max(widest, fm.stringWidth(ln));

        int padX = 14, padY = 10;
        int boxW = widest + padX * 2, boxH = lines.length * lineH + padY * 2;
        int boxX = GAME_WIDTH / 2 - boxW / 2;

        g2.setColor(new Color(16, 20, 30, 210));
        g2.fillRoundRect(boxX, topY, boxW, boxH, 10, 10);
        g2.setColor(new Color(110, 190, 255, 90));
        g2.drawRoundRect(boxX, topY, boxW, boxH, 10, 10);

        int ty = topY + padY + fm.getAscent();
        for (String ln : lines) {
            // The blank is highlighted so the gap reads as the thing to fill.
            int blank = ln.indexOf(Question.BLANK);
            if (blank >= 0) {
                int bx = boxX + padX + fm.stringWidth(ln.substring(0, blank));
                int bw = fm.stringWidth(Question.BLANK);
                g2.setColor(new Color(255, 230, 60, 45));
                g2.fillRoundRect(bx - 2, ty - fm.getAscent() + 2, bw + 4, lineH - 3, 5, 5);
            }
            g2.setColor(blank >= 0 ? new Color(255, 230, 120) : new Color(214, 226, 240));
            g2.drawString(ln, boxX + padX, ty);
            ty += lineH;
        }
        return boxH;
    }

    /**
     * The route, split into the three difficulty sectors. The numbers index
     * Planet.ALL, which stays in orbital order.
     *
     * The tour launches from Earth and works outward to Neptune, then doubles
     * back through the inner planets so the two hottest worlds sit in the hard
     * sector directly before the star. The order is deliberate: a planet's
     * position in its sector matters more than its own trait, because the last
     * planet of every sector falls inside the lane-shuffle zone. Earth has to
     * come first for "clear skies, standard conditions" to be true of it, and
     * Mercury's 25% shorter timer belongs at the end rather than on level 1.
     */
    static final int[][] PLANET_GROUPS = { {2, 3, 4}, {5, 6, 7}, {1, 0} };
    /**
     * How many levels each difficulty sector runs for.
     *
     * Drop this to 3 to split each sector one level per planet, which puts all
     * eight hand-overs within a few seconds of each other for testing. It
     * cannot go below 3: a group holds up to three planets, and a smaller
     * sector would skip whichever ones its slots never land on. Set it back to
     * 50 for the real journey - the regression suite assumes the full route.
     */
    static final int PLANET_SECTOR_SIZE = 50;
    static final int CYCLE_LENGTH = PLANET_SECTOR_SIZE * PLANET_GROUPS.length;

    static int sectorForLevel(int lvl) {
        int idx0 = lvl - 1;
        return (idx0 / PLANET_SECTOR_SIZE) % PLANET_GROUPS.length;
    }

    static int planetIndexForLevel(int lvl) {
        int idx0 = lvl - 1;
        int[] group = PLANET_GROUPS[sectorForLevel(lvl)];
        int posInSector = idx0 % PLANET_SECTOR_SIZE;
        int within = Math.min(posInSector * group.length / PLANET_SECTOR_SIZE, group.length - 1);
        return group[within];
    }

    static final int[] PLANET_START_LEVEL = new int[8];
    /** Each planet's last level: answering it right finishes the planet. */
    static final int[] PLANET_END_LEVEL = new int[8];
    static {
        Arrays.fill(PLANET_START_LEVEL, -1);
        for (int lvl = 1; lvl <= CYCLE_LENGTH; lvl++) {
            int p = planetIndexForLevel(lvl);
            if (PLANET_START_LEVEL[p] == -1) PLANET_START_LEVEL[p] = lvl;
            PLANET_END_LEVEL[p] = lvl;
        }
    }

    static int planetEndLevel(int planet) { return PLANET_END_LEVEL[planet]; }

    /**
     * The planets in the order the route visits them, flattened out of
     * PLANET_GROUPS. Level select and the in-play track lay their stops out
     * along this rather than along Planet.ALL: the two agreed only while the
     * journey happened to follow orbital order, and it no longer does.
     */
    static final int[] TRAVEL_ORDER = new int[8];
    static {
        int slot = 0;
        for (int[] group : PLANET_GROUPS) {
            for (int p : group) TRAVEL_ORDER[slot++] = p;
        }
    }

    /** The planet reached last, just before the star. */
    static int finalPlanet() { return TRAVEL_ORDER[TRAVEL_ORDER.length - 1]; }

    static final int SHUFFLE_ZONE_START = 30;
    static boolean isShuffleZone(int lvl) {
        // At the star the lanes never settle.
        if (isStarLevel(lvl)) return true;
        int posInSector = ((lvl - 1) % PLANET_SECTOR_SIZE) + 1;
        return posInSector >= SHUFFLE_ZONE_START;
    }

    /**
     * TESTING ONLY - set back to false for the real journey.
     *
     * Cuts the route's final planet down to a single level so the hand-over to
     * the star is a few seconds away instead of twenty-five levels away. Every
     * planet before it is untouched; only the end of the run moves.
     */
    static final boolean SHORT_LAST_PLANET = false;

    /**
     * The star sits one level past the eight planets and is the real end of the
     * journey. Levels beyond it exist only for a player who tells the star they
     * want to keep learning.
     */
    static final int STAR_LEVEL = SHORT_LAST_PLANET
        ? PLANET_START_LEVEL[finalPlanet()] + 1
        : CYCLE_LENGTH + 1;

    /**
     * The star is not one question but a gauntlet: fifty drawn at random from
     * every bank, easy to hard, as a summary of the whole journey.
     */
    static final int STAR_QUESTIONS = 50;
    static final int STAR_END_LEVEL = STAR_LEVEL + STAR_QUESTIONS - 1;

    /** Which of the star's questions this level is, counting from 1. */
    int starQuestionNumber() { return clampInt(level - STAR_LEVEL + 1, 1, STAR_QUESTIONS); }

    /** True once the whole gauntlet is behind the player. */
    boolean pastTheGauntlet() { return level > STAR_END_LEVEL; }

    static boolean isStarLevel(int lvl) { return lvl >= STAR_LEVEL; }

    boolean atStar() { return isStarLevel(level); }

    /**
     * Which destination a level belongs to: 0-7 for the planets, or STAR_DEST
     * for the star. Used to notice when the player crosses from one to the next.
     */
    static final int STAR_DEST = 8;

    static int destinationForLevel(int lvl) {
        return isStarLevel(lvl) ? STAR_DEST : planetIndexForLevel(lvl);
    }

    /**
     * The destination standing at a given stop along the route, the star being
     * the last. Screens that lay destinations out left to right count in stops;
     * everything else works in destination ids.
     */
    static int destinationAtSlot(int slot) {
        return slot == STAR_DEST ? STAR_DEST : TRAVEL_ORDER[clampInt(slot, 0, TRAVEL_ORDER.length - 1)];
    }

    /** Which stop a destination stands at - the inverse of destinationAtSlot. */
    static int slotForDestination(int dest) {
        if (dest == STAR_DEST) return STAR_DEST;
        for (int slot = 0; slot < TRAVEL_ORDER.length; slot++) {
            if (TRAVEL_ORDER[slot] == dest) return slot;
        }
        return 0;
    }

    int difficultySector() { return atStar() ? PLANET_GROUPS.length - 1 : sectorForLevel(level); }
    int planetIndex() { return atStar() ? planets.size() - 1 : planetIndexForLevel(level); }
    Planet currentPlanet() { return atStar() ? Planet.STAR : planets.get(planetIndex()); }

    int gameStartLevel = 1;
    int gameOverIndex = 0;
    int victoryIndex = 0;
    String[] victoryItems = { "Fly On", "Play Again", "Main Menu" };

    int highestLevelReached = 1;
    /**
     * Set once the star's gauntlet is cleared. highestLevelReached stops at the
     * star's last question, so it cannot tell "on the last one" from "done".
     */
    boolean journeyComplete = false;
    /** True for the run that first earned Starborn, so the ending can show it off. */
    boolean starbornNewThisRun = false;
    /** Earned but not yet looked at in Settings: the Ship row wears a NEW badge until then. */
    boolean starbornUnseen = false;

    /**
     * Testing escape hatch, like UNLOCK_ALL_LEVELS: every paint is selectable
     * without earning it. Off unless the game is launched with
     * -Drocketlearn.unlockSkins=true (or -Drocketlearn.unlockAll=true), so a
     * normal launch can never give the paints away.
     */
    static final boolean UNLOCK_ALL_SKINS = Boolean.getBoolean("rocketlearn.unlockSkins") || Boolean.getBoolean("rocketlearn.unlockAll");



    EngineTrail currentTrail() {
        return EngineTrail.ALL.get(clampInt(trailIndex, 0, EngineTrail.ALL.size() - 1));
    }




    /**
     * Testing escape hatch. Setting this to true makes every planet in Select
     * Level playable regardless of how far the player has actually travelled,
     * which is useful for checking the later planets without grinding to them.
     * Off unless the game is launched with -Drocketlearn.unlockLevels=true (or
     * -Drocketlearn.unlockAll=true), so a normal launch keeps real progression.
     */
    static final boolean UNLOCK_ALL_LEVELS = Boolean.getBoolean("rocketlearn.unlockLevels") || Boolean.getBoolean("rocketlearn.unlockAll");

    boolean isPlanetLocked(int planetIndex) {
        if (UNLOCK_ALL_LEVELS) return false;
        return PLANET_START_LEVEL[planetIndex] > highestLevelReached;
    }

    String[] gameOverItems() {
        return gameOverActions().stream().map(a -> switch (a) {
            case "continue" -> "Continue from Level " + level;
            case "restart" -> "Restart from Level " + gameStartLevel;
            case "retry" -> "Retry the " + planets.get(challengePlanet).name + " Challenge";
            case "review" -> "Review Mistakes (" + missedQuestions.size() + ")";
            case "scores" -> "High Scores";
            default -> "Main Menu";
        }).toArray(String[]::new);
    }

    // ---- medal challenge -------------------------------------------------

    /**
     * The planet a medal challenge is flying, or -1 outside one. A challenge
     * replays one finished planet from its first level in a single go, only
     * to earn a better medal there: it ends when the planet is finished,
     * moves no progress on, and a game over offers a retry, not a continue.
     */
    int challengePlanet = -1;

    boolean inChallenge() { return challengePlanet >= 0; }

    /** Only a planet already finished can be challenged; the star is the journey's end, not a medal. */
    boolean canChallenge(int dest) {
        return dest != STAR_DEST && !isDestinationLocked(dest) && planetRecords.finished(dest, highestLevelReached);
    }

    void startChallenge(int planet) {
        startGame(PLANET_START_LEVEL[planet]);
        // Set after startGame, which clears it.
        challengePlanet = planet;
    }

    /** The challenge's planet is finished: back to Level Select on it, by way of the score table. */
    void endChallenge() {
        int planet = challengePlanet;
        levelSelectIndex = slotForDestination(planet);
        finishRun(State.LEVEL_SELECT);
        challengePlanet = -1;
        syncMusic();
    }

    List<String> gameOverActions() {
        List<String> a = new ArrayList<>();
        if (inChallenge()) {
            a.add("retry");
        } else {
            a.add("continue");
            a.add("restart");
        }
        if (!missedQuestions.isEmpty()) a.add("review");
        if (!highScores.isEmpty()) a.add("scores");
        a.add("menu");
        return a;
    }

    /**
     * Practice runs the same questions with no hearts, no score and no death -
     * a wrong answer just explains itself and moves on. When lifetime stats
     * show a clear weak topic, practice leans toward questions from it.
     */
    void startPractice() { startPractice(stats.weakestTopic()); }

    /** Practice that leans toward one topic, or mixes them all when it is null. */
    void startPractice(String focus) {
        startGame(Math.max(1, lastLevel));
        // Set after startGame, which clears the practice fields.
        practiceMode = true;
        practiceFocus = focus;
        hearts = startingHearts;
        redrawFirstQuestion();
    }

    /**
     * Drills only the notebook's uncleared questions, ending once each has been
     * answered right on the first try. Does nothing when there are none.
     */
    /**
     * Decides how much to ease the coming question. A topic the player gets
     * right first time under half the time (over enough answers to be sure)
     * earns a quarter more time and slower rocks; so, less, does flying on
     * the last heart. Off in Settings, and never in Flight School.
     */
    void updateAssist() {
        assist = 1.0;
        assistReason = null;
        if (!adaptiveHelp || flightSchoolRun || currentQuestion == null) return;
        int[] t = stats.byTopic.get(currentQuestion.topic);
        if (t != null && t[1] >= WEAK_TOPIC_SAMPLES && t[0] * 100 < t[1] * WEAK_TOPIC_PERCENT) {
            assist = WEAK_TOPIC_ASSIST;
            assistReason = "Extra time: " + currentQuestion.topic + " is one of your harder topics";
        } else if (!practiceMode && startingHearts > 1 && hearts == 1) {
            assist = LAST_HEART_ASSIST;
            assistReason = "Extra time on your last heart";
        }
    }

    /**
     * Reads the new question aloud when Read Aloud is on, and with it the
     * answers from left to right, in the lanes they arrive in.
     */
    void speakQuestion() {
        if (readAloud == READ_OFF || currentQuestion == null) return;
        StringBuilder sb = new StringBuilder(currentQuestion.prompt);
        if (currentQuestion.hasCode()) sb.append(". The code is on screen.");
        if (readAloud == READ_ALL && !options.isEmpty()) {
            List<AnswerOption> lanes = new ArrayList<>(options);
            lanes.sort(Comparator.comparingDouble(o -> o.x));
            sb.append(". Answers, left to right: ");
            for (int i = 0; i < lanes.size(); i++) sb.append(i == 0 ? "" : ", ").append(lanes.get(i).text);
        }
        Speech.say(sb.toString());
    }

    /** The guided first flight: one step at a time, under practice rules. */
    void startFlightSchool() {
        startGame(1);
        practiceMode = true;
        flightSchoolRun = true;
        flightSchool.start();
        hearts = startingHearts;
        options.clear();
        currentQuestion = FlightSchool.QUESTION;
        questionText = FlightSchool.QUESTION.prompt;
    }

    /** Today's ten questions, the same for everyone. Practice rules: no hearts lost, every miss explained. */
    void startDailyFlight() {
        startGame(Math.max(1, lastLevel));
        practiceMode = true;
        dailyRun = true;
        daily.start(dealer.bank);
        hearts = startingHearts;
        redrawFirstQuestion();
    }

    void startMistakesDrill() {
        if ((notebook.practiceCount() == 0)) return;
        startGame(Math.max(1, lastLevel));
        practiceMode = true;
        mistakesDrill = true;
        hearts = startingHearts;
        redrawFirstQuestion();
    }

    /**
     * startGame has already dealt a question before the practice fields were
     * set, so a practice run deals its first one again under its own rules.
     */
    void redrawFirstQuestion() {
        repeatQuestion = false;
        nextQuestion();
    }

    /**
     * Clears whatever a run leaves on screen: shots, wreckage, flashes and any
     * banner. Shared by starting a run and leaving one, so a new effect only
     * needs adding here to be cleared both ways.
     */
    void clearRunEffects() {
        exploding = false;
        rocketAlive = true;
        bannerTimer = 0;
        pendingAction = BannerAction.NONE;
        bullets.clear();
        particles.clear();
        powerUps.clear();
        debris.clear();
        hitFlashes.clear();
        freezeTimer = 0;
        hitStopTimer = 0;
        damageFlash = 0;
        explosionFlash = 0;
        shieldFlash = 0;
        heartPunch = 0;
        lostHeartIndex = -1;
        warping = false;
    }

    /** Lets go of every flight key and settles the throttle, so nothing held carries into the next screen. */
    void releaseControls() {
        leftHeld = false;
        rightHeld = false;
        boostHeld = false;
        slowHeld = false;
        throttle = 0;
    }

    void clearPracticeMode() {
        practiceMode = false;
        practiceFocus = null;
        mistakesDrill = false;
        dailyRun = false;
        flightSchoolRun = false;
    }

    void startGame(int atLevel) {
        starbornNewThisRun = false;
        challengePlanet = -1;
        clearPracticeMode();
        clearRunEffects();
        gameStartLevel = atLevel;
        hearts = startingHearts; score = 0; level = atLevel;
        // Judging starts on whichever planet the run begins; only a start at its
        // first level can go on to earn silver or gold there.
        int startPlanet = isStarLevel(atLevel) ? -1 : planetIndexForLevel(atLevel);
        planetRecords.startPlanet(startPlanet, startPlanet >= 0 && atLevel == PLANET_START_LEVEL[startPlanet]);
        rocketX = GAME_WIDTH / 2.0; rocketVX = 0; rocketY = ROCKET_BASE_Y;
        dealer.reset();
        repeatQuestion = false; missCount = 0; correctStreak = 0; showQuestionPanel = true;
        hasShield = false;
        missedQuestions.clear();
        topicMisses.clear();
        beyondTheStar = false;
        starIntro = false;
        starReaction = 0;
        starTaunt = null;
        starTauntTimer = 0;
        runAnswered = 0;
        runCorrect = 0;
        windLean = 0;
        displayScore = 0;
        scorePop = 0;
        lastTickSecond = -1;
        state = State.PLAYING;
        syncMusic();
        nextQuestion();
    }

    void returnToMenu() {
        Speech.stop();
        // Before practice mode is cleared, which is what tells it to record nothing.
        rememberProgress();
        clearRunEffects();
        releaseControls();
        clearPracticeMode();
        challengePlanet = -1;
        rocketVX = 0;
        menuRocketX = MENU_ROCKET_START_X;
        menuRocketY = MENU_ROCKET_BASE_Y;
        menuRocketVX = 0;
        menuIndex = 0;
        state = State.MENU;
        saveFile.saveProgress();
        syncMusic();
    }

    void nextQuestion() {
        bullets.clear();
        rocketX = GAME_WIDTH / 2.0;
        rocketVX = 0;
        rocketY = ROCKET_BASE_Y;
        rocketAlive = true;
        releaseControls();
        showQuestionPanel = true;
        if (!repeatQuestion) {
            dealer.pickQuestion();
            missCount = 0;
        }
        // A rising chime the round the right answer first lights up, so the
        // player notices the change even while dodging.
        if (missCount == HINT_AFTER_MISSES) playSfx(Sfx.HINT);
        updateAssist();
        maxTime = (fastTimer ? 20.0 : 30.0) * currentPlanet().timeScale * assist;
        timeLeft = maxTime;
        timeExpired = false;
        powerUps.clear();
        freezeTimer = 0;
        warping = false;
        lastTickSecond = -1;
        starFlareCharge = 0;
        starFlareActive = 0;
        starFlareTimer = 4.5;
        hitFlashes.clear();
        debris.clear();
        for (int i = 0; i < currentPlanet().debrisCount; i++) spawnDebris(true);
        powerUpSpawnTimer = rng.nextDouble() < POWERUP_CHANCE ? 1.5 + rng.nextDouble() * 3.0 : -1;
        syncMusic();   // crossing into the star switches to its theme
        spawnOptions();
        speakQuestion();
    }

    static final String[] DIFFICULTY_NAMES = { "EASY", "MEDIUM", "HARD" };
    static final Color[] DIFFICULTY_COLORS = { new Color(80, 220, 100), Color.ORANGE, Color.RED };

    String difficultyName() { return atStar() ? "MIXED" : DIFFICULTY_NAMES[difficultySector()]; }
    Color difficultyColor() { return atStar() ? new Color(255, 215, 0) : DIFFICULTY_COLORS[difficultySector()]; }

    void spawnOptions() {
        options.clear();
        int n = currentQuestion.choices.length;
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < n; i++) idx.add(i);
        Collections.shuffle(idx, rng);
        int slotW = GAME_WIDTH / n;
        // Tuned against BalanceCheck. The binding constraint is not the timer but
        // how long the correct answer stays above the rocket, so asteroids get a
        // long lead-in and a gentler speed - otherwise a player who reads at a
        // normal pace loses a heart before they can even start flying.
        double speed = 38;
        speed /= assist;   // Adaptive Help: eased questions fall slower too
        // Scaling this lead-in by the planet's gravity, so that a rock spends the
        // same number of seconds above the rocket everywhere, was measured against
        // BalanceCheck and made no difference: the pilot already stretches the
        // approach with the slow throttle, so the height it starts from is not
        // what binds. Left as a flat distance.
        for (int i = 0; i < n; i++) {
            int choiceIndex = idx.get(i);
            String text = currentQuestion.choices[choiceIndex];
            boolean correct = choiceIndex == currentQuestion.correct;
            double x = slotW * i + slotW / 2.0;
            double y = -140 - rng.nextInt(200);
            options.add(new AnswerOption(text, correct, x, y, speed, rng, currentPlanet().accent));
        }
        shuffleTimer = SHUFFLE_INTERVAL;
    }

    static final double SHUFFLE_INTERVAL = 1.8;
    static final double SHUFFLE_MOVE_SPEED = 160;
    double shuffleTimer = 0;

    /**
     * Rocks below this line have stopped moving between lanes.
     *
     * The shuffle is there to stop the player lining a shot up the moment the
     * question appears, and that job is done while the rocks are still high.
     * Carrying it on all the way down does something else: it takes a rock out
     * from under a player who has already committed a run at it, which is how a
     * question gets lost despite the answer being known.
     *
     * Measured against BalanceCheck on fixed seeds, settling takes five points
     * off Jupiter's "lost anyway" and four off the star's - the two places where
     * a heavy fall and a late lane swap combine. Mercury and Neptune are not
     * helped, because what costs them is a short clock and a shoved rocket
     * rather than the lane moving.
     */
    static final double SHUFFLE_SETTLE_Y = GAME_HEIGHT * 0.55;

    void updateLaneShuffle(double dt) {
        if (isShuffleZone(level) && options.size() > 1) {
            shuffleTimer -= dt;
            if (shuffleTimer <= 0) {
                shuffleTimer = SHUFFLE_INTERVAL;
                swapTwoLanes();
            }
        }
        // A rock already on its way across finishes the move rather than
        // stopping between lanes.
        for (AnswerOption o : options) {
            if (o.x == o.targetX) continue;
            double move = SHUFFLE_MOVE_SPEED * dt;
            double diff = o.targetX - o.x;
            o.x += Math.abs(diff) <= move ? diff : Math.signum(diff) * move;
        }
    }

    /**
     * Swaps the lanes of two rocks that are both still high enough to be fair
     * game. With fewer than two of those left, the lanes simply hold.
     */
    void swapTwoLanes() {
        List<AnswerOption> high = new ArrayList<>();
        for (AnswerOption o : options) {
            if (o.y < SHUFFLE_SETTLE_Y) high.add(o);
        }
        if (high.size() < 2) return;
        int i = rng.nextInt(high.size());
        int j;
        do { j = rng.nextInt(high.size()); } while (j == i);
        AnswerOption a = high.get(i), b = high.get(j);
        double tmp = a.targetX;
        a.targetX = b.targetX;
        b.targetX = tmp;
    }

    String toastText = null;
    Color toastColor = Color.WHITE;
    double toastTimer = 0;

    void showToast(String text, Color color) {
        toastText = text; toastColor = color; toastTimer = 1.8;
    }

    void spawnDebris(boolean scattered) {
        double x = 50 + rng.nextDouble() * (GAME_WIDTH - 100);
        double y = scattered ? rng.nextDouble() * GAME_HEIGHT * 0.6 - GAME_HEIGHT * 0.6 : -40 - rng.nextDouble() * 120;
        debris.add(new Debris(x, y, 55 + rng.nextDouble() * 45, 11 + rng.nextDouble() * 9, rng));
    }

    void updateDebris(double dt, boolean frozen) {
        if (debris.isEmpty()) return;
        for (Debris d : debris) {
            if (!frozen) d.y += d.speed * dt;
            d.rotation += d.rotSpeed * dt * (frozen ? 0.15 : 1);
        }
        int before = debris.size();
        debris.removeIf(d -> d.y > GAME_HEIGHT + 40);
        for (int i = debris.size(); i < before; i++) spawnDebris(false);

        for (Iterator<Bullet> bi = bullets.iterator(); bi.hasNext(); ) {
            Bullet b = bi.next();
            for (Iterator<Debris> di = debris.iterator(); di.hasNext(); ) {
                Debris d = di.next();
                double dx = b.x - d.x, dy = b.y - d.y;
                if (dx * dx + dy * dy < d.radius * d.radius) {
                    bi.remove();
                    di.remove();
                    triggerRockBurst(d.x, d.y, new Color(130, 120, 110));
                    spawnDebris(false);
                    break;
                }
            }
        }
    }

    void updateDust(double dt) {
        if (!currentPlanet().dust) return;
        dustTimer -= dt;
        if (dustTimer > 0) return;
        // Lower graphics settings blow a thinner storm; the wind itself is unchanged.
        dustTimer = 0.03 / graphics.detail;
        boolean fromLeft = Math.sin(flameFlicker * 0.05) >= 0;
        double vx = (fromLeft ? 1 : -1) * (120 + rng.nextDouble() * 180);
        int tint = 150 + rng.nextInt(60);
        particles.add(new Particle(
            fromLeft ? -10 : GAME_WIDTH + 10,
            rng.nextDouble() * GAME_HEIGHT,
            vx, (rng.nextDouble() - 0.5) * 30,
            2.0 + rng.nextDouble(),
            new Color(tint, tint - 55, tint - 95, 255), 0.03));
    }

    void spawnPowerUp() {
        List<PowerType> allowed = new ArrayList<>();
        for (PowerType t : PowerType.values()) {
            if (t == PowerType.SHIELD && hasShield) continue;
            if (t == PowerType.FREEZE && freezeTimer > 0) continue;
            if (t == PowerType.FIFTY_FIFTY && countWrongOptions() < 2) continue;
            allowed.add(t);
        }
        if (allowed.isEmpty()) return;
        PowerType type = allowed.get(rng.nextInt(allowed.size()));
        double x = 70 + rng.nextDouble() * (GAME_WIDTH - 140);
        powerUps.add(new PowerUp(type, x, -40, 75, rng));
    }

    int countWrongOptions() {
        int n = 0;
        for (AnswerOption o : options) if (!o.correct) n++;
        return n;
    }

    void collectPowerUp(PowerUp pu) {
        for (int i = 0; i < 20; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 40 + rng.nextDouble() * 140;
            Color c = rng.nextBoolean() ? pu.color() : Color.WHITE;
            particles.add(new Particle(pu.x, pu.y, Math.cos(ang) * spd, Math.sin(ang) * spd, 0.35 + rng.nextDouble() * 0.3, c, 0.2));
        }
        switch (pu.type) {
            case SHIELD -> {
                hasShield = true;
                showToast("SHIELD UP - survives one mistake", pu.color());
            }
            case FREEZE -> {
                freezeTimer = FREEZE_DURATION;
                showToast("ASTEROIDS FROZEN - " + (int) FREEZE_DURATION + "s", pu.color());
            }
            default -> {
                int removed = 0;
                for (Iterator<AnswerOption> it = options.iterator(); it.hasNext() && removed < 2; ) {
                    AnswerOption o = it.next();
                    if (o.correct) continue;
                    triggerRockBurst(o.x, o.y, o.color);
                    it.remove();
                    removed++;
                }
                showToast("50/50 - " + removed + " wrong answers destroyed", pu.color());
            }
        }
        playSfx(Sfx.pickupSound(pu.type));
    }

    void shoot() {
        if (exploding || bannerTimer > 0 || fireCooldown > 0 || timeExpired) return;
        fireCooldown = 0.28;
        bullets.add(new Bullet(rocketX, rocketY - rocketH / 2.0, 620));
        muzzleFlash = 0.08;
        recoil = 1.0;
        playSfx(Sfx.laserShot(currentSkin().laser));
        if (flightSchoolRun) flightSchool.shotFired();
    }

    void triggerExplosion() {
        exploding = true;
        rocketAlive = false;
        explodeTimer = 0.6;
        shakeTime = 0.35;
        shakeMag = 9;
        hitStopTimer = 0.09;
        explosionFlash = 0.14;
        damageFlash = 0.55;
        playSfx(Sfx.EXPLOSION);

        for (int i = 0; i < 10; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 30 + rng.nextDouble() * 110;
            Color c = rng.nextBoolean() ? new Color(255, 255, 230) : new Color(255, 235, 150);
            particles.add(new Particle(rocketX, rocketY, Math.cos(ang) * spd, Math.sin(ang) * spd, 0.15 + rng.nextDouble() * 0.15, c));
        }
        for (int i = 0; i < 30; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 60 + rng.nextDouble() * 240;
            Color c = rng.nextBoolean() ? Color.ORANGE : Color.RED;
            particles.add(new Particle(rocketX, rocketY, Math.cos(ang) * spd, Math.sin(ang) * spd, 0.5 + rng.nextDouble() * 0.4, c));
        }
        for (int i = 0; i < 14; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 15 + rng.nextDouble() * 55;
            int gray = 55 + rng.nextInt(45);
            Color c = new Color(gray, gray, gray);
            particles.add(new Particle(rocketX, rocketY, Math.cos(ang) * spd, Math.sin(ang) * spd - 30, 0.8 + rng.nextDouble() * 0.5, c, 0.15));
        }
    }

    void triggerRockBurst(double x, double y, Color base) {
        for (int i = 0; i < 14; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 40 + rng.nextDouble() * 160;
            Color c = rng.nextBoolean() ? base.brighter() : base.darker();
            particles.add(new Particle(x, y, Math.cos(ang) * spd, Math.sin(ang) * spd, 0.35 + rng.nextDouble() * 0.35, c));
        }
    }

    void showBanner(String text, Color color, double duration, BannerAction action, boolean emphasis, String subtitle) {
        bannerText = text; bannerColor = color; bannerTimer = duration; bannerDuration = duration;
        pendingAction = action; bannerEmphasis = emphasis; bannerSubtitle = subtitle;
        bannerHidePanel = false;
        bannerAwaitInput = false;
        bannerExplains = false;
    }

    /** Carries out whatever the banner was waiting to do. */
    void runBannerAction() {
        BannerAction action = pendingAction;
        pendingAction = BannerAction.NONE;
        switch (action) {
            case NEXT_QUESTION -> nextQuestion();
            case GAME_OVER -> { gameOverIndex = 0; finishRun(State.GAME_OVER); }
            case VICTORY -> { victoryIndex = 0; finishRun(State.VICTORY); }
            case SHOW_ARRIVAL -> showArrivalBanner();
            case PLANET_FINISHED -> rewards.showPlanetFinishedBanner();
            case CHALLENGE_DONE -> endChallenge();
            case STAR_INTRO -> starEnding.openStarScreen(true);
            case STAR_REACHED -> starEnding.openStarScreen(false);
            default -> { }
        }
    }

    /** Dismisses a banner that is waiting on the player. */
    void dismissBanner() {
        if (bannerTimer <= 0 || !bannerAwaitInput) return;
        bannerAwaitInput = false;
        bannerTimer = 0;
        playSfx(Sfx.CONFIRM);
        runBannerAction();
    }

    void triggerHeartHeal(double x, double y) {
        for (int i = 0; i < 18; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 30 + rng.nextDouble() * 90;
            Color c = rng.nextBoolean() ? new Color(255, 90, 120) : new Color(255, 160, 180);
            particles.add(new Particle(x, y, Math.cos(ang) * spd, Math.sin(ang) * spd - 40, 0.5 + rng.nextDouble() * 0.35, c));
        }
    }

    void onCorrect() {
        if (flightSchoolRun) {
            flightSchool.answeredRight();
            return;
        }
        int completedLevel = level;
        boolean firstTryAnswer = missCount == 0;
        if (missCount > 0 && currentQuestion != null) {
            scheduleReview(currentQuestion, dealer.currentTier);
            // Got there on a retry: partly learned, but still to clear first try.
            notebook.recordRecovery(currentQuestion);
        }
        // Shooting the glowing rock is following the hint, not knowing the answer.
        boolean hinted = hintActive();
        // What the planet looked like before this answer, to tell what it newly earns.
        int highestBefore = highestLevelReached;
        if (currentQuestion != null && !practiceMode) planetRecords.recordAnswer(missCount == 0);
        boolean clearedNow = false;
        boolean reviewedNow = false;
        if (currentQuestion != null && missCount == 0) {
            stats.record(currentQuestion.topic, true);
            runAnswered++;
            runCorrect++;
            reviewedNow = notebook.isDueForReview(currentQuestion);
            clearedNow = notebook.recordClear(currentQuestion);
        }
        starReact(FACE_RAGE);
        if (isStarLevel(completedLevel) && completedLevel <= STAR_END_LEVEL) {
            maybeTaunt(completedLevel - STAR_LEVEL + 1);
        }
        hitStopTimer = 0.05;
        scorePop = 0.45;
        shakeTime = Math.max(shakeTime, 0.14);
        shakeMag = Math.max(shakeMag, 3);
        int base = 10 * level;
        int timeBonus = (int) (timeLeft * 2);
        double boostMult = 1.0 + Math.max(0, throttle);
        int gained = (int) Math.round((base + timeBonus) * boostMult);
        if (hinted) gained /= 2;
        score += gained;
        if (score > highScore) highScore = score;

        String breakdown = "base " + base + "  +  time " + timeBonus
            + (boostMult > 1.01 ? String.format("  x  boost %.1f", boostMult) : "")
            + (hinted ? "  x  0.5 hinted  -  no streak credit" : "");
        if (clearedNow) {
            int left = notebook.toClearCount();
            breakdown += "\nCleared from your notebook  -  " + (left == 0 ? "none left to clear" : left + " left to clear");
        } else if (reviewedNow) {
            long days = notebook.daysUntilReview(currentQuestion);
            breakdown += "\nReview passed  -  " + (days < 0 ? "learned for good" : "next check in " + days + (days == 1 ? " day" : " days"));
        }

        options.clear();
        bullets.clear();
        powerUps.clear();
        debris.clear();
        level++;
        rememberProgress();
        repeatQuestion = false;
        missCount = 0;
        if (!hinted) correctStreak++;

        // The daily flight is over after its tenth question.
        if (dailyRun) {
            daily.answered(firstTryAnswer && !hinted);
            if (daily.finished()) {
                boolean counted = daily.record();
                playSfx(Sfx.VICTORY_SOUND);
                saveFile.saveProgress();
                showBanner("DAILY FLIGHT DONE", new Color(255, 215, 90), 2.6, BannerAction.GAME_OVER, true,
                    daily.rightFirstTry + " of " + daily.questions.size() + " right first try"
                        + (counted ? "\nStreak: " + daily.streak + (daily.streak == 1 ? " day" : " days") + "  (best " + daily.bestStreak + ")"
                            : "\nAlready flown today - this one was extra practice"));
                bannerAwaitInput = true;
                return;
            }
        }

        // The drill is over once nothing is left to clear.
        if (mistakesDrill && (notebook.practiceCount() == 0)) {
            playSfx(Sfx.VICTORY_SOUND);
            saveFile.saveProgress();
            showBanner("ALL MISTAKES CLEARED!", new Color(80, 220, 100), 2.6,
                BannerAction.GAME_OVER, true, "Every question in your notebook answered right first try");
            return;
        }

        // Clearing the last of the star's fifty ends the journey: the star speaks.
        if (completedLevel == STAR_END_LEVEL) {
            playSfx(Sfx.VICTORY_SOUND);
            if (!journeyComplete) {
                journeyComplete = true;
                starbornNewThisRun = true;
                starbornUnseen = true;
                breakdown += "\nNew ship paint unlocked: Starborn";
            }
            rememberProgress();
            saveFile.saveProgress();
            showBanner("YOU REACHED THE STAR", new Color(255, 232, 150), 2.4,
                BannerAction.STAR_REACHED, true, breakdown);
            return;
        }

        // Flying out of a planet finishes it: judge the medal, line up its own
        // banner, and start judging the next one, met at its first level.
        boolean finishedPlanet = !practiceMode && !isStarLevel(completedLevel)
            && destinationForLevel(completedLevel) != destinationForLevel(level);
        if (finishedPlanet) {
            rewards.finishPlanet(planetIndexForLevel(completedLevel), highestBefore);
            planetRecords.startPlanet(isStarLevel(level) ? -1 : planetIndexForLevel(level), true);
            saveFile.saveProgress();
        }

        boolean healed = !hinted && correctStreak % HEAL_STREAK == 0 && hearts < startingHearts;
        if (healed) {
            hearts++;
            triggerHeartHeal(rocketX, rocketY);
            playSfx(Sfx.HEAL);
        } else {
            playSfx(Sfx.CORRECT);
        }

        boolean arrived = destinationForLevel(completedLevel) != destinationForLevel(level);
        // Reaching the star hands straight over to it rather than to a banner.
        boolean reachingStar = arrived && destinationForLevel(level) == STAR_DEST;
        String text = healed ? "CORRECT! +" + gained + "   ❤ HEART RESTORED!" : "CORRECT! +" + gained;
        double duration = healed ? 1.6 : 1.3;
        BannerAction next = reachingStar ? BannerAction.STAR_INTRO
            : arrived ? BannerAction.SHOW_ARRIVAL : BannerAction.NEXT_QUESTION;
        if (finishedPlanet) {
            // The medal gets a banner of its own once this one has had its moment.
            // A challenge ends there rather than flying on to the next planet.
            rewards.afterPlanetFinished = inChallenge() ? BannerAction.CHALLENGE_DONE : next;
            next = BannerAction.PLANET_FINISHED;
        }
        showBanner(text, new Color(80, 220, 100), duration, next, true, breakdown);
    }

    /**
     * Called when a run ends. Sends the player to the initials screen when the
     * score makes the board, otherwise straight to the given screen.
     */
    void finishRun(State next) {
        rememberProgress();
        if (practiceMode) {
            saveFile.saveProgress();
            returnToMenu();
            return;
        }
        if (highScores.qualifies(score)) {
            nameEntry[0] = nameEntry[1] = nameEntry[2] = 'A';
            nameEntryIndex = 0;
            newHighScoreRank = -1;
            highScoresReturnState = next;
            state = State.NAME_ENTRY;
        } else {
            state = next;
        }
        saveFile.saveProgress();
    }

    void submitHighScore() {
        playSfx(Sfx.CONFIRM);
        newHighScoreRank = highScores.add(new String(nameEntry), score, level);
        saveFile.saveProgress();
        highScoresIndex = 0;
        state = State.HIGH_SCORES;
    }

    void showArrivalBanner() {
        Planet p = currentPlanet();
        showBanner("NOW APPROACHING " + p.name.toUpperCase(), p.base, 3.0, BannerAction.NEXT_QUESTION, true,
            p.traitName + " - " + p.traitDesc + "\n" + p.fact);
        bannerHidePanel = true;
        warping = true;
    }

    /**
     * Whether the miss just counted should be explained, by the Explanations
     * setting: every miss, or only the one that lights up the right asteroid.
     * Game over always explains unless they are off, or the player would never
     * learn why the run ended.
     */
    boolean explainsThisMiss(boolean gameOver) {
        if (currentQuestion == null || !explanationsOn()) return false;
        return practiceMode || gameOver || explainMode == ExplainMode.EVERY || missCount == HINT_AFTER_MISSES;
    }

    /**
     * Puts the explanation under the banner just shown, when this miss earns
     * one, and holds it there until the player presses on. The right answer is
     * named only once it is being given away anyway - by the glow, at game over,
     * or in practice - so an early explanation does not hand over the retry.
     */
    void explainMiss(boolean gameOver) {
        if (!explainsThisMiss(gameOver)) return;
        boolean answerGiven = practiceMode || gameOver || hintActive();
        bannerSubtitle = (answerGiven ? "Correct answer:  " + currentQuestion.correctAnswer() + "\n" : "") + currentQuestion.why;
        bannerAwaitInput = true;
        bannerHidePanel = true;   // the explanation is the point; clear the way for it
        bannerExplains = true;
        if (readAloud != READ_OFF) Speech.say(bannerSubtitle);
    }

    void spawnShieldShards() {
        for (int i = 0; i < 22; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 70 + rng.nextDouble() * 170;
            Color c = rng.nextBoolean() ? new Color(150, 225, 255) : new Color(225, 245, 255);
            particles.add(new Particle(
                rocketX + Math.cos(ang) * 30, rocketY + Math.sin(ang) * 30,
                Math.cos(ang) * spd, Math.sin(ang) * spd,
                0.35 + rng.nextDouble() * 0.3, c, 0.35));
        }
    }

    void recordMistake() {
        if (currentQuestion == null) return;
        if (!missedQuestions.contains(currentQuestion)) missedQuestions.add(currentQuestion);
        topicMisses.merge(currentQuestion.topic, 1, Integer::sum);
        stats.record(currentQuestion.topic, false);
        notebook.recordMiss(currentQuestion);
        runAnswered++;
        starReact(FACE_SMUG);
        // It does not comment on every slip - only often enough to sting.
        if (atStar() && rng.nextDouble() < 0.45) {
            sayTaunt(TAUNT_ON_MISS[rng.nextInt(TAUNT_ON_MISS.length)]);
        }
    }

    /** Answers given in this run, for the report at the end of it. */
    int runAnswered = 0, runCorrect = 0;

    int runAccuracy() {
        return runAnswered == 0 ? 0 : (int) Math.round(runCorrect * 100.0 / runAnswered);
    }

    /** The topic the player has missed most this run, or null if they made no mistakes. */
    String weakestTopic() {
        String worst = null;
        int most = 0;
        for (Map.Entry<String, Integer> e : topicMisses.entrySet()) {
            if (e.getValue() > most) { most = e.getValue(); worst = e.getKey(); }
        }
        return worst;
    }

    /** How often the weakest topic was missed: the most misses of any topic, or 0 with none. */
    int weakestTopicMisses() {
        return topicMisses.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Puts a question the player got wrong back into the pool a few draws from
     * the end, so it comes round again while the explanation is still fresh
     * instead of disappearing for the rest of the run.
     */
    void scheduleReview(Question q, int sector) {
        List<Question> pool = dealer.pools.get(sector);
        int pos = Math.max(0, pool.size() - REVIEW_GAP);
        pool.add(pos, q);
    }

    void loseLife(String label, String detail, String gameOverText) {
        missCount++;
        correctStreak = 0;
        recordMistake();
        powerUps.clear();

        if (hasShield) {
            hasShield = false;
            repeatQuestion = true;
            shakeTime = 0.25;
            shakeMag = 6;
            hitStopTimer = 0.07;
            shieldFlash = 0.45;
            spawnShieldShards();
            playSfx(Sfx.EXPLOSION);
            showBanner(label + " SHIELD ABSORBED IT", new Color(90, 200, 255), 2.4, BannerAction.NEXT_QUESTION, false, null);
            explainMiss(false);
            return;
        }

        if (practiceMode) {
            // Practice never kills you. It holds the explanation on screen for as
            // long as the player needs, then continues on their key press.
            repeatQuestion = true;
            shakeTime = 0.2;
            shakeMag = 5;
            hitStopTimer = 0.06;
            showBanner(label + " here's why", Color.ORANGE, 1, BannerAction.NEXT_QUESTION, false, null);
            explainMiss(false);
            return;
        }

        hearts--;
        planetRecords.recordHeartLost();
        lostHeartIndex = hearts;
        heartPunch = 0.5;
        triggerExplosion();
        if (hearts <= 0) {
            playSfx(Sfx.GAME_OVER_SOUND);
            rememberProgress();
            saveFile.saveProgress();
            showBanner(gameOverText, Color.RED, 2.6, BannerAction.GAME_OVER, false, null);
            explainMiss(true);
        } else {
            repeatQuestion = true;
            String msg = hintActive()
                ? label + " -1 HEART - watch for the glowing chip"
                : label + " -1 HEART - " + detail;
            showBanner(msg, Color.ORANGE, 2.4, BannerAction.NEXT_QUESTION, false, null);
            explainMiss(false);
        }
    }

    void onWrong() { loseLife("WRONG!", "same question again", "BOOM! GAME OVER"); }

    void onMissed() { loseLife("MISSED!", "it flew right past you", "BOOM! GAME OVER"); }

    void onTimeout() { loseLife("TIME'S UP!", "same question again", "TIME'S UP! GAME OVER"); }

    /**
     * One tick by hand, then a paint request: what the tests use to step the
     * game. While playing, the GameLoop calls frame() instead.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        step();
        repaint();
    }

    /** Frames the GameLoop has run since the game started. */
    volatile long framesRun = 0;

    /** One frame from the GameLoop: step the game, then paint it straight away so frames stay evenly spaced. */
    void frame() {
        framesRun++;
        step();
        if (isShowing()) paintImmediately(0, 0, getWidth(), getHeight());
    }

    /** Advances everything by the time since the last step. */
    void step() {
        long now = System.nanoTime();
        double dt = (now - lastNanoTime) / 1_000_000_000.0;
        lastNanoTime = now;
        if (dt > 0.05) dt = 0.05;
        flameFlicker += dt * 20;
        noteState();
        if (fadeFromState != state) {
            fadeFromState = state;
            fadeTimer = FADE_TIME;
        } else if (fadeTimer > 0) {
            fadeTimer -= dt;
        }
        // Cosmetic timers decay on every screen, so a flash cannot freeze
        // on-screen when the player pauses or ducks into Settings.
        updateEffects(dt);
        updateMusicDuck(dt);
        input.pollGamepad(dt);
        switch (state) {
            case PLAYING -> update(dt);
            case MENU -> updateMenuRocket(dt);
            case STAR -> starEnding.updateStarScreen(dt);
            default -> { }
        }
        trimParticles();
        // Landing on the Ship or Trail row, or a codex card, counts as having seen what is new there.
        rewards.noteSeen();
        input.updateCursor();
    }

    /** How far the music dips while the player reads, and how fast it gets there. */
    static final double DUCK_LEVEL = 0.35, DUCK_SPEED = 3.0;
    double musicDuck = 1.0;

    /** Moments meant for reading: an explanation, the star's speech, a list of mistakes. */
    boolean readingMoment() {
        if (state == State.PLAYING) return bannerTimer > 0 && bannerExplains;
        if (state == State.STAR) return !starEnding.starFinishedSpeaking();
        return state == State.REVIEW || state == State.NOTEBOOK;
    }

    /** Eases the music down under reading moments and back up after them. */
    void updateMusicDuck(double dt) {
        double target = readingMoment() ? DUCK_LEVEL : 1.0;
        double step = DUCK_SPEED * dt;
        musicDuck = musicDuck < target ? Math.min(target, musicDuck + step) : Math.max(target, musicDuck - step);
        Sfx.setMusicDuck(musicDuck);
    }

    static double menuAsteroidX(double[] a, double t) {
        return ((a[0] + a[3] * t) % (GAME_WIDTH + 60) + GAME_WIDTH + 60) % (GAME_WIDTH + 60) - 30;
    }

    static double menuAsteroidY(double[] a, double t) {
        return ((a[1] + a[4] * t) % (GAME_HEIGHT + 60) + GAME_HEIGHT + 60) % (GAME_HEIGHT + 60) - 30;
    }

    void menuShoot() {
        if (fireCooldown > 0) return;
        fireCooldown = 0.28;
        bullets.add(new Bullet(menuRocketX, menuRocketY - rocketH * MENU_ROCKET_SCALE / 2.0, 620));
        playSfx(Sfx.laserShot(currentSkin().laser));
    }

    void updateMenuBullets(double dt) {
        if (fireCooldown > 0) fireCooldown -= dt;
        double t = flameFlicker / 20.0;

        for (Iterator<Bullet> bi = bullets.iterator(); bi.hasNext(); ) {
            Bullet b = bi.next();
            b.y -= b.speed * dt;
            if (b.y < -20) { bi.remove(); continue; }

            for (double[] a : menuAsteroids) {
                double ax = menuAsteroidX(a, t), ay = menuAsteroidY(a, t);
                double dx = b.x - ax, dy = b.y - ay, hitR = a[2] + 4;
                if (dx * dx + dy * dy < hitR * hitR) {
                    bi.remove();
                    triggerRockBurst(ax, ay, new Color(150, 130, 112));
                    playSfx(Sfx.CLICK);
                    a[0] = rng.nextDouble() * GAME_WIDTH - a[3] * t;
                    a[1] = -40 - a[4] * t;
                    a[2] = 10 + rng.nextDouble() * 14;
                    break;
                }
            }
        }
    }

    /** Eases the throttle toward where the held keys point it: quick to open, slower to settle. */
    void stepThrottle(double dt) {
        double target = throttleTarget();
        double rate = target != 0 ? 1.0 / THROTTLE_RAMP_UP : 1.0 / THROTTLE_RAMP_DOWN;
        if (throttle < target) throttle = Math.min(target, throttle + rate * dt);
        else if (throttle > target) throttle = Math.max(target, throttle - rate * dt);
    }

    /** How high the rocket rides for the current throttle: up toward top on boost, down toward bottom on slow. */
    double rideY(double base, double top, double bottom) {
        return throttle >= 0 ? base + (top - base) * throttle : base + (bottom - base) * -throttle;
    }

    /** Boost flame or slow-down smoke trailing from the rocket's tail at (x, y). */
    void emitExhaust(double x, double y, double dt) {
        if (throttle > 0.5 && rng.nextDouble() < (throttle - 0.5) * 2 * dt * 25) {
            double ang = Math.PI / 2 + (rng.nextDouble() - 0.5) * 0.5;
            double spd = 70 + rng.nextDouble() * 90;
            Color c = rng.nextBoolean() ? new Color(255, 200, 120) : new Color(160, 225, 255);
            particles.add(new Particle(x, y, Math.cos(ang) * spd * 0.3, Math.sin(ang) * spd, 0.2 + rng.nextDouble() * 0.2, c));
        } else if (throttle < -0.5 && rng.nextDouble() < (-throttle - 0.5) * 2 * dt * 6) {
            int gray = 70 + rng.nextInt(30);
            particles.add(new Particle(x, y, (rng.nextDouble() - 0.5) * 20, 20 + rng.nextDouble() * 20, 0.5 + rng.nextDouble() * 0.3, new Color(gray, gray, gray), 0.15));
        }
    }

    /** Whether two boxes, each given by its centre and size, overlap. */
    static boolean overlaps(double ax, double ay, double aw, double ah, double bx, double by, double bw, double bh) {
        return Math.abs(ax - bx) * 2 < aw + bw && Math.abs(ay - by) * 2 < ah + bh;
    }

    void updateMenuRocket(double dt) {
        updateMenuBullets(dt);
        stepThrottle(dt);

        double maxSpd = ROCKET_MAX_SPEED * MENU_SPEED_SCALE;
        double accel = 700, friction = 0.85;
        menuRocketVX = steer(menuRocketVX, accel, friction, dt);
        menuRocketVX = clamp(menuRocketVX, -maxSpd, maxSpd);
        menuRocketX += menuRocketVX * dt;

        double margin = rocketW * MENU_ROCKET_SCALE * 0.75;
        if (menuRocketX < margin) { menuRocketX = margin; menuRocketVX = 0; }
        else if (menuRocketX > GAME_WIDTH - margin) { menuRocketX = GAME_WIDTH - margin; menuRocketVX = 0; }

        menuRocketY = rideY(MENU_ROCKET_BASE_Y, MENU_ROCKET_TOP_Y, MENU_ROCKET_BOTTOM_Y);

        double exhaustY = menuRocketY + rocketH * MENU_ROCKET_SCALE / 2.0;
        emitExhaust(menuRocketX, exhaustY, dt);
        ship.emitStarbornSparks(menuRocketX, exhaustY, rocketW * MENU_ROCKET_SCALE, dt);
        ship.emitTrail(menuRocketX, exhaustY, rocketW * MENU_ROCKET_SCALE, dt);

        for (Particle p : particles) p.update(dt);
        particles.removeIf(p -> p.life <= 0);
    }

    /** Decays the purely cosmetic timers. Runs even during hit-stop. */
    void updateEffects(double dt) {
        if (muzzleFlash > 0) muzzleFlash -= dt;
        if (recoil > 0) recoil = Math.max(0, recoil - dt * 6);
        if (damageFlash > 0) damageFlash -= dt;
        if (explosionFlash > 0) explosionFlash -= dt;
        if (shieldFlash > 0) shieldFlash -= dt;
        if (scorePop > 0) scorePop -= dt;
        if (starReaction > 0) starReaction -= dt;
        if (starTauntTimer > 0) {
            starTauntTimer -= dt;
            if (starTauntTimer <= 0) starTaunt = null;
        }
        if (heartPunch > 0) {
            heartPunch -= dt;
            if (heartPunch <= 0) lostHeartIndex = -1;
        }
        displayScore += (score - displayScore) * (1 - Math.exp(-9.0 * dt));
        if (Math.abs(score - displayScore) < 0.6) displayScore = score;

        for (HitFlash f : hitFlashes) f.life -= dt;
        hitFlashes.removeIf(f -> f.life <= 0);
    }

    void update(double dt) {
        // Hit-stop: hold the simulation for a few frames so impacts land.
        if (hitStopTimer > 0) {
            hitStopTimer -= dt;
            return;
        }
        if (flightSchoolRun) flightSchool.tick();

        if (fireCooldown > 0) fireCooldown -= dt;

        stepThrottle(dt);

        if (!exploding) {
            Planet planet = currentPlanet();
            double maxSpd = rocketMaxSpeed();
            double accel = fastTimer ? 900 * 1.15 : 900, friction = planet.frictionValue;
            rocketVX = steer(rocketVX, accel, friction, dt);
            if (planet.windAccel > 0) {
                windPhase += dt;
                double gust = Math.sin(windPhase * 0.6);
                rocketVX += planet.windAccel * gust * dt;
                windLean = gust * 0.22;
            } else {
                windLean = 0;
            }

            boolean icy = planet.frictionValue > 0.9;
            if (icy && steerDir() == 0 && Math.abs(rocketVX) > 70 && rng.nextDouble() < dt * 22) {
                double side = Math.signum(rocketVX);
                particles.add(new Particle(
                    rocketX - side * rocketW * 0.5, rocketY + rocketH * 0.35,
                    -side * (30 + rng.nextDouble() * 50), -10 - rng.nextDouble() * 25,
                    0.3 + rng.nextDouble() * 0.25,
                    new Color(215, 240, 255), 0.25));
            }
            rocketVX = clamp(rocketVX, -maxSpd, maxSpd);
            rocketX += rocketVX * dt;
            // Stop dead at a wall, so pulling away does not first have to undo
            // the speed built up pressing into it.
            double minX = rocketW / 2.0, maxX = GAME_WIDTH - rocketW / 2.0;
            if (rocketX <= minX) { rocketX = minX; rocketVX = Math.max(0, rocketVX); }
            else if (rocketX >= maxX) { rocketX = maxX; rocketVX = Math.min(0, rocketVX); }

            rocketY = rideY(ROCKET_BASE_Y, ROCKET_TOP_Y, ROCKET_BOTTOM_Y);

            emitExhaust(rocketX, rocketY + rocketH / 2.0, dt);
            ship.emitStarbornSparks(rocketX, rocketY + rocketH / 2.0, rocketW, dt);
            ship.emitTrail(rocketX, rocketY + rocketH / 2.0, rocketW, dt);
        }

        Iterator<Bullet> bit = bullets.iterator();
        while (bit.hasNext()) {
            Bullet b = bit.next();
            b.y -= b.speed * dt;
            if (b.y < -20) bit.remove();
        }

        if (toastTimer > 0) toastTimer -= dt;
        if (freezeTimer > 0) freezeTimer -= dt;
        boolean frozen = freezeTimer > 0;

        double asteroidMult = frozen ? 0 : asteroidSpeedMultiplier();
        for (AnswerOption o : options) { o.y += o.speed * asteroidMult * dt; o.rotation += o.rotationSpeed * dt * (frozen ? 0.15 : 1); }
        if (!frozen) updateLaneShuffle(dt);
        options.removeIf(o -> o.y > GAME_HEIGHT + 40);

        if (powerUpSpawnTimer > 0) {
            powerUpSpawnTimer -= dt;
            if (powerUpSpawnTimer <= 0) {
                powerUpSpawnTimer = -1;
                spawnPowerUp();
            }
        }
        for (PowerUp pu : powerUps) {
            pu.y += pu.speed * dt;
            pu.rotation += 1.6 * dt;
            pu.bob += 4 * dt;
        }
        powerUps.removeIf(pu -> pu.y > GAME_HEIGHT + 40);

        updateDebris(dt, frozen);
        updateDust(dt);
        updateEnvironment(dt);

        for (Particle p : particles) p.update(dt);
        particles.removeIf(p -> p.life <= 0);

        if (exploding) {
            explodeTimer -= dt;
            if (explodeTimer <= 0) exploding = false;
        }

        if (shakeTime > 0) {
            shakeTime -= dt;
            double m = reduceMotion ? 0 : shakeMag * clamp(shakeTime / 0.35, 0, 1);
            shakeX = (rng.nextDouble() * 2 - 1) * m;
            shakeY = (rng.nextDouble() * 2 - 1) * m;
        } else {
            shakeX = 0; shakeY = 0;
        }

        if (bannerTimer > 0) {
            // A banner waiting on the player holds until they press to continue.
            if (!bannerAwaitInput) {
                bannerTimer -= dt;
                if (bannerTimer <= 0) runBannerAction();
            }
            return;
        }

        if (exploding) return;

        for (Iterator<PowerUp> pi = powerUps.iterator(); pi.hasNext(); ) {
            PowerUp pu = pi.next();
            boolean hit = false;
            for (Iterator<Bullet> bi = bullets.iterator(); bi.hasNext(); ) {
                Bullet b = bi.next();
                if (overlaps(b.x, b.y, 0, 0, pu.x, pu.y, POWERUP_W, POWERUP_H)) {
                    bi.remove();
                    hit = true;
                    break;
                }
            }
            if (!hit && overlaps(rocketX, rocketY, rocketW, rocketH, pu.x, pu.y, POWERUP_W, POWERUP_H)) {
                hit = true;
            }
            if (hit) {
                pi.remove();
                collectPowerUp(pu);
                hitStopTimer = Math.max(hitStopTimer, 0.04);
                shakeTime = Math.max(shakeTime, 0.12);
                shakeMag = Math.max(shakeMag, 3);
            }
        }

        outer:
        for (Iterator<Bullet> bi = bullets.iterator(); bi.hasNext(); ) {
            Bullet b = bi.next();
            for (Iterator<AnswerOption> oi = options.iterator(); oi.hasNext(); ) {
                AnswerOption o = oi.next();
                if (overlaps(b.x, b.y, 0, 0, o.x, o.y, CHIP_W, CHIP_H)) {
                    bi.remove();
                    hitFlashes.add(new HitFlash(playfield.buildAsteroidPath(o), 0.11));
                    oi.remove();
                    triggerRockBurst(o.x, o.y, o.color);
                    if (o.correct) onCorrect(); else onWrong();
                    break outer;
                }
            }
        }

        if (bannerTimer <= 0 && !exploding) {
            for (Iterator<AnswerOption> oi = options.iterator(); oi.hasNext(); ) {
                AnswerOption o = oi.next();
                if (overlaps(rocketX, rocketY, rocketW, rocketH, o.x, o.y, CHIP_W, CHIP_H)) {
                    oi.remove();
                    triggerRockBurst(o.x, o.y, o.color);
                    if (o.correct) onCorrect(); else onWrong();
                    break;
                }
            }
        }

        if (bannerTimer <= 0 && !exploding) {
            for (Iterator<AnswerOption> oi = options.iterator(); oi.hasNext(); ) {
                AnswerOption o = oi.next();
                if (o.correct && o.y - CHIP_H / 2.0 > rocketY + rocketH / 2.0) {
                    oi.remove();
                    onMissed();
                    break;
                }
            }
        }

        if (bannerTimer <= 0) {
            if (!timeExpired && !frozen) {
                double timeRate = 1.0 + Math.max(0, -throttle);
                timeLeft -= dt * timeRate;
                if (timeLeft <= 0) {
                    timeLeft = 0;
                    timeExpired = true;
                }
                // One dry tick per second over the last five.
                int sec = (int) Math.ceil(timeLeft);
                if (timeLeft <= 5.0 && sec != lastTickSecond) {
                    lastTickSecond = sec;
                    playSfx(Sfx.TICK);
                }
            }
            if (timeExpired && bullets.isEmpty()) onTimeout();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        long paintStart = System.nanoTime();
        paintTimes.addLast(paintStart);
        while (paintStart - paintTimes.peekFirst() > 1_000_000_000L) paintTimes.removeFirst();
        super.paintComponent(g);
        Graphics2D screen = (Graphics2D) g;

        int panelW = getWidth(), panelH = getHeight();
        screen.setColor(Color.BLACK);
        screen.fillRect(0, 0, panelW, panelH);
        double scale = Math.min(panelW / (double) GAME_WIDTH, panelH / (double) GAME_HEIGHT);
        double offX = (panelW - GAME_WIDTH * scale) / 2.0;
        double offY = (panelH - GAME_HEIGHT * scale) / 2.0;
        // The mouse maps through the on-screen view, however the frame was drawn.
        viewScale = scale; viewOffX = offX; viewOffY = offY;

        // Drawing costs by the pixel, so on a big screen the lower graphics
        // settings draw the frame smaller and stretch it to fit.
        boolean gpu = gpuStretches();
        double renderScale = gpu || graphics.stretchWithoutGpu ? Math.min(scale, graphics.maxRenderScale) : scale;
        boolean stretched = renderScale < scale;

        Graphics2D g2;
        if (stretched) {
            int bw = (int) Math.ceil(GAME_WIDTH * renderScale), bh = (int) Math.ceil(GAME_HEIGHT * renderScale);
            if (frameBuffer == null || frameBuffer.getWidth() != bw || frameBuffer.getHeight() != bh) {
                // TYPE_INT_RGB: the software loops Java draws into fastest.
                frameBuffer = new java.awt.image.BufferedImage(bw, bh, java.awt.image.BufferedImage.TYPE_INT_RGB);
            }
            g2 = frameBuffer.createGraphics();
            g2.scale(renderScale, renderScale);
        } else {
            frameBuffer = null;
            g2 = screen;
            g2.translate(offX, offY);
            g2.scale(scale, scale);
        }
        applyRenderingHints(g2);

        // Cleared before the shake moves the picture, so its edges never show the last frame.
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        if (shakeX != 0 || shakeY != 0) g2.translate(shakeX, shakeY);

        markAt = System.nanoTime();
        drawScene(g2);
        drawFade(g2);
        if (showFps) drawFpsCounter(g2, stretched ? renderScale : scale);

        if (stretched) {
            g2.dispose();
            // A graphics card smooths the stretch for free; done in software a
            // smooth stretch cost more than drawing the whole frame, so there
            // it is blocky instead.
            screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, gpu
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            screen.drawImage(frameBuffer, (int) Math.round(offX), (int) Math.round(offY),
                (int) Math.round(GAME_WIDTH * scale), (int) Math.round(GAME_HEIGHT * scale), null);
        }
        lastPaintMs = (System.nanoTime() - paintStart) / 1e6;
    }

    /**
     * The F3 readout: frames per second, how long the last frame took to
     * paint, the graphics level and the resolution actually drawn at. Under
     * the shake, so it holds still while the screen moves.
     */
    void drawFpsCounter(Graphics2D g2, double drawScale) {
        AffineTransform tx = g2.getTransform();
        if (shakeX != 0 || shakeY != 0) g2.translate(-shakeX, -shakeY);
        int fps = framesPerSecond();
        String text = fps + " FPS  •  " + String.format("%.1f", lastPaintMs) + " ms/frame  •  "
            + graphics.label + "  •  " + (int) Math.round(GAME_WIDTH * drawScale) + "x" + (int) Math.round(GAME_HEIGHT * drawScale);
        g2.setFont(font(Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 14, x = GAME_WIDTH / 2 - w / 2;
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRoundRect(x, 2, w, 16, 8, 8);
        // Green when smooth, amber when it is starting to drag, red when it drags.
        g2.setColor(fps >= 50 ? new Color(120, 230, 140) : fps >= 30 ? new Color(255, 200, 90) : new Color(255, 110, 100));
        g2.drawString(text, x + 7, 14);
        // Below it, where the time goes: the costliest parts of the frame, in ms.
        if (!drawMs.isEmpty() && state == State.PLAYING) {
            String parts = costliestParts(5);
            int pw = fm.stringWidth(parts) + 14, px = GAME_WIDTH / 2 - pw / 2;
            g2.setColor(new Color(0, 0, 0, 175));
            g2.fillRoundRect(px, 20, pw, 16, 8, 8);
            g2.setColor(new Color(200, 210, 225));
            g2.drawString(parts, px + 7, 32);
        }
        g2.setTransform(tx);
    }

    void applyRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** Whether the screen is drawn by the graphics card; null until the window exists to ask. */
    Boolean gpuScreen;

    /**
     * True when the window is drawn by the graphics card, where stretching a
     * frame is nearly free. Java falls back to drawing in software on some
     * older or badly supported cards, and there a smooth stretch is slow.
     */
    boolean gpuStretches() {
        if (gpuScreen != null) return gpuScreen;
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) return false;
        try {
            java.awt.image.VolatileImage probe = gc.createCompatibleVolatileImage(1, 1);
            gpuScreen = probe.getCapabilities().isAccelerated();
            probe.flush();
        } catch (RuntimeException e) {
            gpuScreen = false;
        }
        return gpuScreen;
    }

    /** Fades the new screen in after every state change. */
    void drawFade(Graphics2D g2) {
        if (fadeTimer <= 0) return;
        int alpha = (int) (clamp(fadeTimer / FADE_TIME, 0, 1) * 255);
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
    }

    void drawScene(Graphics2D g2) {
        drawBackground(g2);

        if (state == State.MENU) {
            menus.drawMenuScreen(g2);
            return;
        }
        if (state == State.PILOTS) {
            pilots.draw(g2);
            return;
        }
        if (state == State.SETTINGS_MENU) {
            settings.drawSettingsMenu(g2);
            return;
        }
        if (state == State.SETTINGS) {
            settings.drawSettingsScreen(g2);
            return;
        }
        if (state == State.CONTROLS) {
            settings.drawControlsScreen(g2);
            return;
        }
        if (state == State.LEVEL_SELECT) {
            menus.drawLevelSelectScreen(g2);
            return;
        }
        if (state == State.HOW_TO_PLAY) {
            menus.drawHowToPlayScreen(g2);
            return;
        }
        if (state == State.HIGH_SCORES) {
            records.drawHighScoresScreen(g2);
            return;
        }
        if (state == State.REVIEW) {
            study.drawReviewScreen(g2);
            return;
        }
        if (state == State.STUDY) {
            study.drawStudyScreen(g2);
            return;
        }
        if (state == State.NOTEBOOK) {
            study.drawNotebookScreen(g2);
            return;
        }
        if (state == State.TOPIC_STATS) {
            study.drawTopicStatsScreen(g2);
            return;
        }
        if (state == State.CODEX) {
            study.drawCodexScreen(g2);
            return;
        }
        if (state == State.STAR) {
            starEnding.drawStarScreen(g2);
            return;
        }

        mark("stars");
        scenery.drawPlanetAtmosphere(g2);
        mark("planet tint");
        scenery.drawEnvironmentBack(g2);
        mark("weather");
        scenery.drawSceneryVignette(g2);
        mark("vignette");
        scenery.drawPlanetBackdrop(g2);
        mark("planet");
        playfield.drawOptions(g2);
        playfield.drawHitFlashes(g2);
        playfield.drawDebris(g2);
        playfield.drawPowerUps(g2);
        playfield.drawBullets(g2);
        if (rocketAlive) ship.drawRocket(g2);
        playfield.drawShieldBubble(g2);
        if (exploding) playfield.drawShockwave(g2);
        playfield.drawParticles(g2);
        mark("playfield");
        scenery.drawEnvironmentFront(g2);
        playfield.drawFreezeOverlay(g2);
        playfield.drawImpactFlashes(g2);
        scenery.drawPlanetTrack(g2);
        mark("effects");
        boolean runOver = state == State.GAME_OVER || state == State.VICTORY || state == State.NAME_ENTRY;
        if (showQuestionPanel && !runOver && !(bannerTimer > 0 && bannerHidePanel)
                && (!flightSchoolRun || flightSchool.asking())) hud.drawQuestionPanel(g2);
        if (flightSchoolRun && bannerTimer <= 0) flightSchool.draw(g2);
        // The ending screens speak for themselves; the playing HUD only clutters them.
        if (state != State.VICTORY && state != State.NAME_ENTRY) hud.drawHUD(g2);

        if (bannerTimer > 0) hud.drawBanner(g2);
        if (state == State.GAME_OVER) menus.drawGameOverScreen(g2);
        if (state == State.VICTORY) starEnding.drawVictoryScreen(g2);
        if (state == State.PAUSED) menus.drawPausedScreen(g2);
        if (state == State.NAME_ENTRY) records.drawNameEntryScreen(g2);
        mark("hud");
    }

    /**
     * Where each frame's drawing time goes, for the F3 readout: milliseconds
     * per part, averaged over recent frames so the figures hold still. Only
     * kept while F3 is on (or profiling is set), so normal play pays nothing.
     * With a graphics card drawing, figures are the time to hand the work
     * over rather than to finish it, so they read low.
     */
    final Map<String, Double> drawMs = new LinkedHashMap<>();
    boolean profiling;
    long markAt;

    void mark(String part) {
        if (!showFps && !profiling) return;
        long now = System.nanoTime();
        double ms = (now - markAt) / 1e6;
        drawMs.merge(part, ms, (old, v) -> old * 0.9 + v * 0.1);
        markAt = now;
    }

    /** The costliest parts of the frame, for the second F3 line. */
    String costliestParts(int count) {
        return drawMs.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(count)
            .map(e -> e.getKey() + " " + String.format("%.1f", e.getValue()))
            .collect(java.util.stream.Collectors.joining("  •  "));
    }

    void drawBackground(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        double t = flameFlicker / 20.0;
        double focusX = (state == State.MENU ? menuRocketX : rocketX) - GAME_WIDTH / 2.0;
        double warp = warpAmount();

        // The stars cycle through the depths in order, so drawing only the
        // first few still leaves near and far ones in the mix.
        int shown = graphics.count(stars.size());
        for (int i = 0; i < shown; i++) {
            double[] s = stars.get(i);
            double depth = s[3];
            double y = (s[1] + t * 14 * depth) % GAME_HEIGHT;
            if (y < 0) y += GAME_HEIGHT;
            double x = (s[0] - focusX * 0.035 * depth) % GAME_WIDTH;
            if (x < 0) x += GAME_WIDTH;
            int alpha = (int) (55 + depth * 165);

            if (warp > 0.01) {
                // Stretch each star into a streak, the near ones travelling furthest.
                double len = warp * (14 + depth * 80);
                g2.setColor(new Color(200, 225, 255, (int) (alpha * (0.5 + warp * 0.5))));
                g2.setStroke(new BasicStroke((float) Math.max(1, s[2] * 0.9)));
                g2.draw(new Line2D.Double(x, y - len, x, y));
                g2.setStroke(new BasicStroke(1f));
            } else {
                g2.setColor(new Color(255, 255, 255, alpha));
                // A star is only a pixel or two across: a square reads the same and fills far faster.
                if (graphics.glows) g2.fill(new Ellipse2D.Double(x, y, s[2], s[2]));
                else g2.fillRect((int) x, (int) y, (int) Math.ceil(s[2]), (int) Math.ceil(s[2]));
            }
        }
    }

    /**
     * How stretched the starfield is, 0 to 1. Ramps up as the arrival banner
     * appears and eases back down before it clears, so the jump between planets
     * reads as travel.
     */
    double warpAmount() {
        if (!warping || bannerTimer <= 0 || bannerDuration <= 0) return 0;
        double progress = 1 - clamp(bannerTimer / bannerDuration, 0, 1);
        return Math.sin(progress * Math.PI);
    }

    State selGlowOwnerState = null;
    double selGlowY, selGlowW;
    long selGlowLastNanos = 0;

    double viewScale = 1, viewOffX = 0, viewOffY = 0;
    State uiListState = null;
    int uiListStartY, uiListGap, uiListCount;
    State uiPlanetState = null;
    int uiPlanetCy, uiPlanetSlotW;

    void drawSelectableList(Graphics2D g2, String[] labels, int selectedIndex, int startY, int gap, int selFontSize, int normalFontSize) {
        drawSelectableList(g2, labels, selectedIndex, startY, gap, selFontSize, normalFontSize, null, null);
    }

    /**
     * As above, but a row whose sliders entry is 0-100 draws as a volume
     * slider (label, speaker, track, percent) instead of plain text.
     */
    void drawSelectableList(Graphics2D g2, String[] labels, int selectedIndex, int startY, int gap, int selFontSize, int normalFontSize,
                            int[] sliders, boolean[] muted) {
        uiListState = state;
        uiListStartY = startY;
        uiListGap = gap;
        uiListCount = labels.length;

        int targetCy = startY + selectedIndex * gap;
        FontMetrics selFm = g2.getFontMetrics(font(Font.BOLD, selFontSize));
        int targetW = SettingsScreen.isSliderRow(sliders, selectedIndex) ? SettingsScreen.SLIDER_ROW_W
            : selFm.stringWidth("> " + labels[selectedIndex] + " <");

        long now = System.nanoTime();
        if (selGlowOwnerState != state) {
            selGlowY = targetCy;
            selGlowW = targetW;
            selGlowOwnerState = state;
        } else {
            double dt = clamp((now - selGlowLastNanos) / 1_000_000_000.0, 0, 0.05);
            double f = 1 - Math.exp(-16.0 * dt);
            selGlowY += (targetCy - selGlowY) * f;
            selGlowW += (targetW - selGlowW) * f;
        }
        selGlowLastNanos = now;

        // The highlight is padded up to 10px, but never so far that it spills
        // onto the rows above and below when a list is packed tight.
        int textH = selFm.getAscent() + selFm.getDescent();
        int pad = clampInt((gap - textH) / 2, 2, 10);
        float pulse = (float) (0.5 + 0.5 * Math.sin(flameFlicker * 1.5));
        g2.setColor(new Color(255, 225, 90, (int) (55 + pulse * 55)));
        g2.fillRoundRect((int) (GAME_WIDTH / 2 - selGlowW / 2 - 18), (int) (selGlowY - selFm.getAscent() - pad), (int) selGlowW + 36, selFm.getAscent() + selFm.getDescent() + pad * 2, 18, 18);

        for (int i = 0; i < labels.length; i++) {
            boolean sel = i == selectedIndex;
            int cy = startY + i * gap;
            g2.setFont(font(Font.BOLD, sel ? selFontSize : normalFontSize));
            g2.setColor(sel ? Color.YELLOW : Color.WHITE);
            if (SettingsScreen.isSliderRow(sliders, i)) {
                settings.drawVolumeSlider(g2, labels[i], sliders[i], muted != null && muted[i], sel, cy);
                continue;
            }
            String label = (sel ? "> " : "") + labels[i] + (sel ? " <" : "");
            drawCentered(g2, label, GAME_WIDTH / 2, cy);
        }
    }

    /**
     * Vertical band the main menu list must stay inside. The tagline sits just
     * above MENU_LIST_TOP and the per-item hint just below MENU_LIST_BOTTOM, so
     * keeping every row between them is what stops the list colliding with
     * either as entries are added.
     */
    static final int MENU_LIST_TOP = 152, MENU_LIST_BOTTOM = 362, MENU_MAX_GAP = 42;

    String practiceFocusHint() {
        String weak = stats.weakestTopic();
        return weak == null
            ? "Drill questions with no hearts and no score"
            : "No hearts, no score - focuses on " + weak;
    }

    int planetsUnlocked() {
        int n = 0;
        for (int i = 0; i < planets.size(); i++) if (!isPlanetLocked(i)) n++;
        return n;
    }

    /** Eight planets plus the star. */
    int destinationCount() { return planets.size() + 1; }

    Planet destinationPlanet(int dest) {
        return dest == STAR_DEST ? Planet.STAR : planets.get(dest);
    }

    int destinationStartLevel(int dest) {
        return dest == STAR_DEST ? STAR_LEVEL : PLANET_START_LEVEL[dest];
    }

    boolean isDestinationLocked(int dest) {
        if (UNLOCK_ALL_LEVELS) return false;
        if (dest == STAR_DEST) return highestLevelReached < STAR_LEVEL;
        return isPlanetLocked(dest);
    }

    /** The topics a destination asks about: its difficulty's bank, or every bank at the star. */
    Set<String> destinationTopics(int dest) {
        Set<String> topics = new TreeSet<>();
        if (dest == STAR_DEST) {
            for (List<Question> tier : dealer.bank) for (Question q : tier) topics.add(q.topic);
        } else {
            for (Question q : dealer.bank.get(sectorForLevel(destinationStartLevel(dest)))) topics.add(q.topic);
        }
        return topics;
    }

    /**
     * One line for Level Select on how the player is doing in this
     * destination's topics, from their lifetime stats: the weakest and the
     * strongest, so they can pick where to go knowing what they will face.
     * Topics answered fewer than Stats.MIN_SAMPLES times are not judged yet.
     */
    String destinationMasteryLine(int dest) {
        Set<String> topics = destinationTopics(dest);
        String weak = null, strong = null;
        int weakPct = 101, strongPct = -1;
        for (String t : topics) {
            int[] s = stats.byTopic.get(t);
            if (s == null || s[1] < Stats.MIN_SAMPLES) continue;
            int pct = stats.topicAccuracy(t);
            if (pct < weakPct) { weakPct = pct; weak = t; }
            if (pct > strongPct) { strongPct = pct; strong = t; }
        }
        if (weak == null) return "Topics here: " + String.join(", ", topics);
        if (weak.equals(strong)) return "Your accuracy here: " + weak + " " + weakPct + "%";
        return "Weakest here: " + weak + " " + weakPct + "%   •   Strongest: " + strong + " " + strongPct + "%";
    }

    // ---- per-destination environments ----------------------------------

    double envPhase = 0;
    double lightningTimer = 2.5;
    double lightningFlash = 0;
    double boltSeed = 0;

    // The star's flare cycle: a telegraphed wind-up, then the flare itself.
    static final double FLARE_CHARGE_TIME = 1.35, FLARE_ACTIVE_TIME = 1.1;
    double starFlareTimer = 4.5;
    double starFlareCharge = 0;
    double starFlareActive = 0;

    /** A short-lived expression after the player scores on the star or misses. */
    double starReaction = 0;
    int starReactionMood = FACE_CALM;

    /** What the star is saying mid-fight, and how long it stays up. */
    String starTaunt = null;
    double starTauntTimer = 0;
    static final double TAUNT_TIME = 3.4;

    /** Milestone lines, keyed by how many of the fifty are behind the player. */
    static final int[] TAUNT_AT = { 5, 10, 20, 25, 30, 40, 45, 49 };
    static final String[] TAUNT_LINES = {
        "Five. You are barely warm.",
        "Ten. I have not even bent my light for you.",
        "Twenty, and still flying. Hm.",
        "Halfway. Do not mistake that for winning.",
        "Thirty. Your hands are getting heavy.",
        "Forty. I can see you thinking now.",
        "Five left. Do not fumble them here.",
        "One more, traveller. One."
    };

    /** Thrown at the player when they get one wrong at the star. */
    static final String[] TAUNT_ON_MISS = {
        "You knew that one. Did you?",
        "Eight worlds, and you forget this?",
        "Burn a little brighter, traveller.",
        "That one was free. The next will not be."
    };

    void sayTaunt(String line) {
        if (practiceMode) return;
        starTaunt = line;
        starTauntTimer = TAUNT_TIME;
    }

    /** Called with how many of the fifty the player has now cleared. */
    void maybeTaunt(int cleared) {
        for (int i = 0; i < TAUNT_AT.length; i++) {
            if (TAUNT_AT[i] == cleared) {
                sayTaunt(TAUNT_LINES[i]);
                return;
            }
        }
    }

    void starReact(int mood) {
        if (!atStar()) return;
        starReactionMood = mood;
        starReaction = 1.2;
    }

    boolean starFlaring() { return starFlareActive > 0; }

    void updateEnvironment(double dt) {
        envPhase += dt;
        if (lightningFlash > 0) lightningFlash -= dt;

        Planet p = currentPlanet();
        if (p.env == Environment.THUNDER) {
            lightningTimer -= dt;
            if (lightningTimer <= 0) {
                lightningTimer = 2.5 + rng.nextDouble() * 4.5;
                lightningFlash = 0.32;
                boltSeed = rng.nextDouble() * 1000;
            }
        }

        if (p.env == Environment.CORONA) {
            updateStarFlares(dt);

            // A constant updraft of embers, far heavier while a flare is erupting.
            double rate = (starFlaring() ? 90 : 20) * graphics.detail;
            if (rng.nextDouble() < dt * rate) {
                double x = rng.nextDouble() * GAME_WIDTH;
                int warm = 170 + rng.nextInt(70);
                double lift = starFlaring() ? 130 + rng.nextDouble() * 180 : 45 + rng.nextDouble() * 80;
                particles.add(new Particle(x, GAME_HEIGHT + 10,
                    (rng.nextDouble() - 0.5) * 40, -lift,
                    1.3 + rng.nextDouble() * 1.2,
                    new Color(255, warm, 60), -0.06));
            }
        } else {
            starFlareCharge = 0;
            starFlareActive = 0;
        }
    }

    /**
     * The star's boss beat. It gathers light for a moment - the warning - then
     * erupts, driving the asteroids down harder until the flare burns out.
     */
    void updateStarFlares(double dt) {
        if (starFlareActive > 0) {
            starFlareActive -= dt;
            if (starFlareActive <= 0) starFlareTimer = 4.0 + rng.nextDouble() * 3.0;
            return;
        }
        if (starFlareCharge > 0) {
            starFlareCharge -= dt;
            if (starFlareCharge <= 0) {
                starFlareActive = FLARE_ACTIVE_TIME;
                shakeTime = Math.max(shakeTime, 0.4);
                shakeMag = Math.max(shakeMag, 7);
                playSfx(Sfx.EXPLOSION);
                for (int i = 0, n = graphics.count(40); i < n; i++) {
                    double x = rng.nextDouble() * GAME_WIDTH;
                    int warm = 180 + rng.nextInt(60);
                    particles.add(new Particle(x, GAME_HEIGHT + 8,
                        (rng.nextDouble() - 0.5) * 90, -(200 + rng.nextDouble() * 220),
                        0.9 + rng.nextDouble() * 0.9,
                        new Color(255, warm, 70), -0.08));
                }
            }
            return;
        }
        starFlareTimer -= dt;
        if (starFlareTimer <= 0) {
            starFlareCharge = FLARE_CHARGE_TIME;
            showToast("SOLAR FLARE INCOMING", new Color(255, 180, 70));
        }
    }

    /**
     * The star itself: a blazing disc with rays that turn slowly, standing in
     * for the planet backdrop once the player passes Neptune.
     */
    static final int FACE_CALM = 0, FACE_SPEAKING = 1, FACE_FIERCE = 2;
    /** Furious - the player just scored on it. */
    static final int FACE_RAGE = 3;
    /** Gloating - the player just missed. */
    static final int FACE_SMUG = 4;

    static final double ORBIT_BASE_Y = GAME_HEIGHT - 14, ORBIT_LIFT = 26;

    static final int TOPIC_ROWS_TOP = 118, TOPIC_ROW_GAP = 33;

    /** Rows on the topic screen: every topic, then Back. */
    int topicStatsRows() { return ALL_TOPICS.size() + 1; }

    // Name-entry geometry, shared by drawing and the mouse so they always agree.
    static final int NAME_SLOT_W = 66, NAME_SLOT_H = 78, NAME_SLOT_GAP = 16;
    static final int NAME_SLOT_TOP = GAME_HEIGHT / 2 - 40;
    static final int NAME_SAVE_W = 140, NAME_SAVE_H = 32, NAME_SAVE_TOP = NAME_SLOT_TOP + NAME_SLOT_H + 34;
    /** nameEntryHit's answer for the SAVE button. */
    static final int NAME_HIT_SAVE = -2;

    static int nameSlotX(int i) {
        int totalW = HighScores.NAME_LENGTH * NAME_SLOT_W + (HighScores.NAME_LENGTH - 1) * NAME_SLOT_GAP;
        return GAME_WIDTH / 2 - totalW / 2 + i * (NAME_SLOT_W + NAME_SLOT_GAP);
    }

    /**
     * What the mouse is over on the name-entry screen: {slot, part} where part
     * is 1 for its up arrow, -1 for its down arrow and 0 for the box itself;
     * {NAME_HIT_SAVE, 0} for SAVE; {-1, 0} for nothing.
     */
    static int[] nameEntryHit(double gx, double gy) {
        if (gx >= GAME_WIDTH / 2 - NAME_SAVE_W / 2 && gx <= GAME_WIDTH / 2 + NAME_SAVE_W / 2
            && gy >= NAME_SAVE_TOP && gy <= NAME_SAVE_TOP + NAME_SAVE_H) return new int[]{ NAME_HIT_SAVE, 0 };
        for (int i = 0; i < HighScores.NAME_LENGTH; i++) {
            int sx = nameSlotX(i);
            if (gx < sx || gx > sx + NAME_SLOT_W) continue;
            if (gy >= NAME_SLOT_TOP - 28 && gy < NAME_SLOT_TOP) return new int[]{ i, 1 };
            if (gy >= NAME_SLOT_TOP && gy <= NAME_SLOT_TOP + NAME_SLOT_H) return new int[]{ i, 0 };
            if (gy > NAME_SLOT_TOP + NAME_SLOT_H && gy <= NAME_SLOT_TOP + NAME_SLOT_H + 28) return new int[]{ i, -1 };
        }
        return new int[]{ -1, 0 };
    }

    /** Left-click on the name-entry screen: pick a box, step a letter, or save. */
    void nameEntryClick(double gx, double gy) {
        int[] hit = nameEntryHit(gx, gy);
        if (hit[0] == NAME_HIT_SAVE) { submitHighScore(); return; }
        if (hit[0] < 0) return;
        nameEntryIndex = hit[0];
        if (hit[1] != 0) nameEntry[hit[0]] = InputHandler.cycleLetter(nameEntry[hit[0]], hit[1]);
        playSfx(Sfx.CLICK);
    }

    /** One notch of scroll on the name-entry screen: the box under the mouse, else the active one. */
    void nameEntryScroll(int dir) {
        int slot = nameEntryHit(mouseGX, mouseGY)[0];
        if (slot >= 0) nameEntryIndex = slot;
        // Scrolling up runs forward through the alphabet, like the up arrow.
        nameEntry[nameEntryIndex] = InputHandler.cycleLetter(nameEntry[nameEntryIndex], -dir);
        playSfx(Sfx.CLICK);
    }

    int nextPlanetStartLevel() {
        int best = -1;
        for (int start : PLANET_START_LEVEL) {
            if (start > level && (best == -1 || start < best)) best = start;
        }
        return best;
    }

    static final int[] LABEL_FONT_SIZES = { 15, 13, 11, 10 };
    static final int[] LABEL_FONT_SIZES_LARGE = { 19, 17, 15, 13, 11, 10 };

    /** A small gold "NEW" pill with its top-left corner at (x, y). */
    void drawNewBadge(Graphics2D g2, int x, int y) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(flameFlicker * 3));
        g2.setFont(font(Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth("NEW") + 12, h = 16;
        g2.setColor(new Color(255, 200, 60, (int) (190 + pulse * 65)));
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setColor(new Color(60, 35, 0));
        g2.drawString("NEW", x + 6, y + h - 4);
    }

    ShipSkin currentSkin() {
        return ShipSkin.ALL.get(clampInt(shipSkinIndex, 0, ShipSkin.ALL.size() - 1));
    }

    void drawCentered(Graphics2D g2, String text, int cx, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text);
        g2.drawString(text, cx - w / 2, y);
    }

    void drawCenteredShadowed(Graphics2D g2, String text, int cx, int y, Color color) {
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text);
        g2.setColor(new Color(0, 0, 0, 190));
        g2.drawString(text, cx - w / 2 + 2, y + 2);
        g2.setColor(color);
        g2.drawString(text, cx - w / 2, y);
    }

    /**
     * Wrapped text by font, rendering context, width and text. The question,
     * its explanation, the banner and every asteroid label are wrapped again
     * each frame although their text changes once a question, so the answer
     * is kept. Bounded, oldest first out, so a long session cannot grow it.
     */
    static final int WRAP_CACHE_SIZE = 256;
    final Map<List<Object>, List<String>> wrapCache = new LinkedHashMap<>(WRAP_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<List<Object>, List<String>> eldest) { return size() > WRAP_CACHE_SIZE; }
    };

    /** Splits text into lines no wider than maxWidth. The list returned is shared: callers only read it. */
    List<String> wrapText(FontMetrics fm, String text, int maxWidth) {
        List<Object> key = List.of(fm.getFont(), fm.getFontRenderContext(), maxWidth, text);
        List<String> hit = wrapCache.get(key);
        if (hit != null) return hit;
        List<String> lines = Collections.unmodifiableList(wrapLines(fm, text, maxWidth));
        wrapCache.put(key, lines);
        return lines;
    }

    static List<String> wrapLines(FontMetrics fm, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (fm.stringWidth(test) > maxWidth && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    int listIndexAt(double gy) {
        if (uiListState != state || uiListCount <= 0) return -1;
        int half = Math.min(uiListGap, 46) / 2;
        for (int i = 0; i < uiListCount; i++) {
            int center = uiListStartY + i * uiListGap - 10;
            if (gy >= center - half && gy <= center + half) return i;
        }
        return -1;
    }

    int planetIndexAt(double gx, double gy) {
        if (uiPlanetState != state || uiPlanetSlotW <= 0) return -1;
        if (gy < uiPlanetCy - 50 || gy > uiPlanetCy + 60) return -1;
        int i = (int) (gx / uiPlanetSlotW);
        return i >= 0 && i < destinationCount() ? i : -1;
    }

    void setSelectionForState(int idx) {
        if (idx != selectionForState()) resetArmed = false;
        switch (state) {
            case MENU -> menuIndex = idx;
            case SETTINGS_MENU -> settingsMenuIndex = idx;
            case SETTINGS -> settingsIndex = idx;
            case CONTROLS -> controlsIndex = idx;
            case PAUSED -> pauseIndex = idx;
            case GAME_OVER -> gameOverIndex = idx;
            case VICTORY -> victoryIndex = idx;
            case STUDY -> studyIndex = idx;
            case TOPIC_STATS -> topicStatsIndex = idx;
            case PILOTS -> pilots.select(idx);
            default -> { }
        }
    }

    int selectionForState() {
        return switch (state) {
            case MENU -> menuIndex;
            case SETTINGS_MENU -> settingsMenuIndex;
            case SETTINGS -> settingsIndex;
            case CONTROLS -> controlsIndex;
            case PAUSED -> pauseIndex;
            case GAME_OVER -> gameOverIndex;
            case VICTORY -> victoryIndex;
            case STUDY -> studyIndex;
            case TOPIC_STATS -> topicStatsIndex;
            case PILOTS -> pilots.index;
            default -> -1;
        };
    }

    /** Screens that are read or typed into rather than picked from a list. */
    boolean mouseInert() {
        return state == State.PLAYING || state == State.NAME_ENTRY
            || state == State.HOW_TO_PLAY || state == State.HIGH_SCORES
            || state == State.REVIEW || state == State.NOTEBOOK || state == State.CODEX || awaitingKeybind;
    }

    /** Where the mouse last was, in game coordinates, for hover highlights. -1 once it leaves the window. */
    double mouseGX = -1, mouseGY = -1;

    /** The list the question browser is showing: this run's mistakes, or the notebook. */
    List<Question> browserList() {
        return state == State.NOTEBOOK ? notebook.entries : missedQuestions;
    }

    /**
     * How long the screens a run can end on ignore "go on" presses. The fire
     * and confirm keys are often still being hammered as a run ends, and
     * without this the first of them would pick whatever option happened to
     * be highlighted, or skip the star's speech, before it was ever read.
     */
    static final double INPUT_GUARD = 0.35;
    State guardSeenState = null;
    long guardSeenNanos = 0;

    /** Notes when the current screen was first seen, from the tick or from input. */
    void noteState() {
        if (guardSeenState != state) {
            guardSeenState = state;
            guardSeenNanos = System.nanoTime();
            resetArmed = false;   // an armed reset never survives leaving its screen
        }
    }

    boolean confirmBlocked() {
        noteState();
        boolean guarded = state == State.GAME_OVER || state == State.VICTORY || state == State.NAME_ENTRY
            || state == State.HIGH_SCORES || state == State.STAR;
        return guarded && (System.nanoTime() - guardSeenNanos) / 1e9 < INPUT_GUARD;
    }

    // ---- controller ------------------------------------------------------
    //
    // The controller mostly speaks through keyPressed, so every screen, the
    // input guard and every future key binding work the same for it as for
    // the keyboard. Only flight is read directly, as held state, because it
    // is steered continuously rather than pressed.

    /**
     * Starts the slow one-off set-up - Java's audio system, the generated
     * sounds and the graphics-card probe, about half a second together - on
     * its own thread, so it overlaps Swing starting up instead of following
     * it. Nothing waits for it: whatever needs one of these first simply
     * waits for that part to finish, as the JVM guarantees for class set-up.
     */
    static void warmUpInBackground() {
        Thread t = new Thread(() -> {
            Sfx.warmUp();
            GraphicsQuality.recommended();
        }, "warm-up");
        t.setDaemon(true);
        t.start();
    }

    /** The game on screen now. A pilot switch replaces it with a fresh one. */
    static volatile RocketLearn current;

    /** Set at launch when more than one pilot shares the computer: the first game opens on "Who's playing?". */
    static boolean askWhoIsPlaying;

    /**
     * Starts as the last pilot chosen on this computer, so their save is the
     * one loaded, and asks who is playing when there is more than one.
     */
    static void startAsLastPilot() {
        Profiles profiles = Profiles.here();
        Profiles.Pilot last = profiles.lastPilot();
        if (last != null) {
            SaveFile.SAVE_FILE = profiles.saveOf(last);
            SaveFile.pilotName = last.name();
        }
        askWhoIsPlaying = profiles.pilots.size() > 1;
    }

    /**
     * Replaces this game with a fresh one reading the active pilot's save:
     * every field starts from scratch, so nothing of one pilot's run can leak
     * into the next. This game saves once more, then never again.
     */
    RocketLearn reborn() {
        saveFile.saveProgress();
        saveFile.retired = true;
        timer.stop();
        RocketLearn next = new RocketLearn();
        if (frame != null) {
            frame.remove(this);
            frame.add(next);
            next.setFrame(frame);
            next.fullscreen = fullscreen;
            frame.revalidate();
            next.requestFocusInWindow();
        }
        current = next;
        return next;
    }

    public static void main(String[] args) {
        warmUpInBackground();
        startAsLastPilot();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("RocketLearn");
            RocketLearn game = new RocketLearn();
            current = game;
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(game);
            game.setFrame(frame);
            frame.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    current.requestFocusInWindow();
                }
            });

            game.fullscreen = true;
            applyFullscreen(frame, true, game.vsync);
            game.requestFocusInWindow();
        });
    }
}
