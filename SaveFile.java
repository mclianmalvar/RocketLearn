package rocketlearn;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static rocketlearn.RocketLearn.*;

/**
 * Reads and writes the player's progress and settings to rocketlearn.save.
 */
class SaveFile {

    final RocketLearn game;

    /** The file this game reads and writes: the active pilot's save when the game was made. */
    final File file;

    /** Set once a pilot switch has replaced this game: from then on it never writes, not even at exit. */
    volatile boolean retired;

    SaveFile(RocketLearn game) {
        this.game = game;
        this.file = SAVE_FILE;
    }

    static final String NAME = "rocketlearn.save";

    /** Where the first pilot's save lives: beside the game, or the player's own folder when that is read-only. */
    static final File DEFAULT_SAVE = chooseSaveFile(GameFiles.dir(), fallbackDir());

    /** The active pilot's save, which a newly made game reads and writes. Choosing a pilot switches it. */
    static volatile File SAVE_FILE = DEFAULT_SAVE;
    /** The active pilot's name, or null when no pilot has been chosen (a single player, no profiles). */
    static volatile String pilotName;

    /**
     * The save lives beside the game so the folder carries its progress with
     * it. A game folder that cannot be written to - installed under Program
     * Files, or on a locked school machine - would lose every save in silence,
     * so there the player's own application-data folder is used instead.
     */
    static File chooseSaveFile(File gameDir, File fallback) {
        return canWriteIn(gameDir) ? new File(gameDir, NAME) : new File(fallback, NAME);
    }

    /** Tests a folder by writing a scratch file into it, which is the only check Windows answers honestly. */
    static boolean canWriteIn(File dir) {
        try {
            File probe = File.createTempFile("rocketlearn-write", ".tmp", dir);
            if (!probe.delete()) probe.deleteOnExit();
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    /** The player's own folder for the game: %APPDATA%\RocketLearn on Windows, ~/.rocketlearn elsewhere. */
    static File fallbackDir() {
        String appData = System.getenv("APPDATA");
        return appData != null && !appData.isEmpty()
            ? new File(appData, "RocketLearn")
            : new File(System.getProperty("user.home"), ".rocketlearn");
    }

    /**
     * The file to read progress from: the save if there is one, otherwise a
     * save left beside the game, so moving to the fallback folder keeps it.
     */
    File readableSave() {
        if (file.exists()) return file;
        // Only the first pilot's save can have been left beside the game.
        if (!file.getName().equals(NAME)) return file;
        File besideGame = GameFiles.file(NAME);
        return besideGame.exists() ? besideGame : file;
    }

    /**
     * A copy of the last save that loaded cleanly, refreshed at each launch.
     * When the save itself turns out damaged, progress comes back from here
     * instead of the player silently starting over.
     */
    static File backupOf(File save) { return new File(save.getPath() + ".bak"); }

    /** Where a damaged save is set aside, so the next save cannot write over the only copy. */
    static File brokenOf(File save) { return new File(save.getPath() + ".broken"); }

    /** Shown on the main menu when the save could not be read; null when all is well. */
    String status = null;

    /**
     * Reads a save, or returns null when it is damaged. Properties forgives
     * almost anything, so a cut-off or blanked file reads as empty rather than
     * failing: a save without the key every save writes counts as damaged too.
     */
    static Properties read(File file) {
        Properties p = new Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            p.load(in);
        } catch (Exception e) {
            return null;
        }
        return p.getProperty("highestLevel") == null ? null : p;
    }

    void loadProgress() {
        File source = readableSave();
        if (!source.exists()) return;
        Properties p = read(source);
        if (p != null) {
            copyQuietly(source, backupOf(source));
        } else {
            copyQuietly(source, brokenOf(source));
            p = read(backupOf(source));
            // Kept short: the menu shows it in the corner beside the title.
            status = p != null ? "Save damaged - backup restored" : "Save damaged - started fresh";
            if (p == null) return;
        }

        int version = readInt(p, "saveVersion", 1);
        if (version > SAVE_VERSION) status = "Save is from a newer RocketLearn";

        game.highestLevelReached = clampInt(readInt(p, "highestLevel", 1), 1, STAR_END_LEVEL);
        game.lastLevel = clampInt(readInt(p, "lastLevel", 1), 1, game.highestLevelReached);
        game.journeyComplete = Boolean.parseBoolean(p.getProperty("journeyComplete", "false"));
        game.starbornUnseen = game.journeyComplete && Boolean.parseBoolean(p.getProperty("starbornUnseen", "false"));
        game.highScore = Math.max(0, readInt(p, "highScore", 0));
        game.startingHearts = clampInt(readInt(p, "startingHearts", MIN_HEARTS), MIN_HEARTS, MAX_HEARTS);
        game.fastTimer = Boolean.parseBoolean(p.getProperty("fastTimer", "false"));
        game.soundOn = Boolean.parseBoolean(p.getProperty("soundOn", "true"));
        game.musicOn = Boolean.parseBoolean(p.getProperty("musicOn", "true"));
        game.masterVolume = clampInt(readInt(p, "masterVolume", 100), 0, 100);
        game.musicVolume = clampInt(readInt(p, "musicVolume", 100), 0, 100);
        game.effectsVolume = clampInt(readInt(p, "effectsVolume", 100), 0, 100);
        game.shipSkinIndex = clampInt(readInt(p, "shipSkin", 0), 0, ShipSkin.ALL.size() - 1);
        game.trailIndex = clampInt(readInt(p, "engineTrail", 0), 0, EngineTrail.ALL.size() - 1);
        game.planetRecords.load(p);
        game.daily.load(p);
        // A player who has already flown past level 1 does not need the lesson offered.
        game.flightSchoolDone = Boolean.parseBoolean(p.getProperty("flightSchoolDone", "false")) || game.highestLevelReached > 1;
        game.rewards.unseen.clear();
        for (String u : p.getProperty("unseen", "").split(",")) if (!u.isBlank()) game.rewards.unseen.add(u.trim());
        game.reduceMotion = Boolean.parseBoolean(p.getProperty("reduceMotion", "false"));
        game.largeText = Boolean.parseBoolean(p.getProperty("largeText", "false"));
        game.adaptiveHelp = Boolean.parseBoolean(p.getProperty("adaptiveHelp", "true"));
        game.highContrast = Boolean.parseBoolean(p.getProperty("highContrast", "false"));
        game.readAloud = clampInt(readInt(p, "readAloud", READ_OFF), 0, READ_LABELS.length - 1);
        game.explainMode = ExplainMode.parse(p.getProperty("explanations"));
        // A save from before the setting existed keeps the computer's own recommendation.
        String graphics = p.getProperty("graphics");
        if (graphics != null) game.graphics = GraphicsQuality.parse(graphics);
        // Only a rate the setting offers is accepted; a save without one keeps the recommendation.
        int savedRate = readInt(p, "frameRate", -1);
        for (int rate : FRAME_RATES) if (rate == savedRate) game.frameRate = rate;
        game.vsync = Boolean.parseBoolean(p.getProperty("vsync", "true"));
        game.highScores.load(p);
        game.stats.load(p);
        game.notebook.load(p);
        game.keyLeft = readInt(p, "keyLeft", game.keyLeft);
        game.keyRight = readInt(p, "keyRight", game.keyRight);
        game.keyShoot = readInt(p, "keyShoot", game.keyShoot);
        game.keyBoost = readInt(p, "keyBoost", game.keyBoost);
        game.keySlow = readInt(p, "keySlow", game.keySlow);
        migrate(version);
        // A save may hold a paint or trail that is not earned under the current
        // rules (or was only selectable under a testing flag).
        game.rewards.keepCosmeticsEarned();
        if (!keysUsable(game.keyLeft, game.keyRight, game.keyShoot, game.keyBoost, game.keySlow)) {
            game.keyLeft = KeyEvent.VK_A;
            game.keyRight = KeyEvent.VK_D;
            game.keyShoot = KeyEvent.VK_SPACE;
            game.keyBoost = KeyEvent.VK_W;
            game.keySlow = KeyEvent.VK_S;
        }
    }

    /**
     * Whether a set of saved keys can actually fly the ship: every one a real
     * key, none of them taken by Esc, F3 or F11 (which the game keeps for
     * itself), and no two actions sharing a key. An edited or damaged save
     * that fails this falls back to the default keys rather than leaving the
     * game unplayable.
     */
    static boolean keysUsable(int... keys) {
        Set<Integer> seen = new HashSet<>();
        for (int k : keys) {
            if (k <= 0 || k > 0xFFFF) return false;
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_F3 || k == KeyEvent.VK_F11) return false;
            if (KeyEvent.getKeyText(k).startsWith("Unknown")) return false;
            if (!seen.add(k)) return false;
        }
        return true;
    }

    /**
     * The save's format, written as "saveVersion". Raise it whenever a change
     * means an older save has to be read differently, and add the step to
     * migrate() rather than guessing from which keys happen to be missing.
     *
     *   1 - no saveVersion key: everything before planet medals. Its
     *       Explanations setting may be a number (0, 1, 2), which
     *       ExplainMode.parse still reads.
     *   2 - planet medals ("medal.<planet>"), engine trails, saveVersion itself.
     */
    static final int SAVE_VERSION = 2;

    /** Brings what was just loaded from an older save up to date. */
    void migrate(int from) {
        if (from < 2) {
            // Medals did not exist yet: every planet already flown past counts
            // as finished, with bronze, so no unlock it had is taken away.
            for (int p = 0; p < game.planetRecords.best.length; p++) {
                if (game.highestLevelReached > planetEndLevel(p) && game.planetRecords.best[p] == PlanetRecords.Medal.NONE) {
                    game.planetRecords.best[p] = PlanetRecords.Medal.BRONZE;
                }
            }
        }
    }

    static void copyQuietly(File from, File to) {
        try { Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException | SecurityException ignored) { }
    }

    /**
     * Saves are made from the game's own thread and, at exit, from the
     * shutdown hook. Synchronized so the two never write at once, and each
     * gathers its snapshot again if the game changed a list mid-copy.
     */
    synchronized void saveProgress() {
        if (retired) return;
        Properties p = null;
        for (int attempt = 0; p == null && attempt < 3; attempt++) {
            try { p = snapshot(); }
            catch (ConcurrentModificationException e) { p = null; }
        }
        if (p == null) return;
        write(p);
    }

    Properties snapshot() {
        Properties p = new Properties();
        p.setProperty("saveVersion", String.valueOf(SAVE_VERSION));
        p.setProperty("highestLevel", String.valueOf(game.highestLevelReached));
        p.setProperty("lastLevel", String.valueOf(game.lastLevel));
        p.setProperty("journeyComplete", String.valueOf(game.journeyComplete));
        p.setProperty("starbornUnseen", String.valueOf(game.starbornUnseen));
        p.setProperty("highScore", String.valueOf(game.highScore));
        p.setProperty("startingHearts", String.valueOf(game.startingHearts));
        p.setProperty("fastTimer", String.valueOf(game.fastTimer));
        p.setProperty("soundOn", String.valueOf(game.soundOn));
        p.setProperty("musicOn", String.valueOf(game.musicOn));
        p.setProperty("masterVolume", String.valueOf(game.masterVolume));
        p.setProperty("musicVolume", String.valueOf(game.musicVolume));
        p.setProperty("effectsVolume", String.valueOf(game.effectsVolume));
        p.setProperty("shipSkin", String.valueOf(game.shipSkinIndex));
        p.setProperty("engineTrail", String.valueOf(game.trailIndex));
        game.planetRecords.save(p);
        game.daily.save(p);
        p.setProperty("flightSchoolDone", String.valueOf(game.flightSchoolDone));
        p.setProperty("unseen", String.join(",", game.rewards.unseen));
        p.setProperty("reduceMotion", String.valueOf(game.reduceMotion));
        p.setProperty("largeText", String.valueOf(game.largeText));
        p.setProperty("adaptiveHelp", String.valueOf(game.adaptiveHelp));
        p.setProperty("highContrast", String.valueOf(game.highContrast));
        p.setProperty("readAloud", String.valueOf(game.readAloud));
        p.setProperty("explanations", game.explainMode.name());
        p.setProperty("graphics", game.graphics.name());
        p.setProperty("frameRate", String.valueOf(game.frameRate));
        p.setProperty("vsync", String.valueOf(game.vsync));
        game.highScores.save(p);
        game.stats.save(p);
        game.notebook.save(p);
        p.setProperty("keyLeft", String.valueOf(game.keyLeft));
        p.setProperty("keyRight", String.valueOf(game.keyRight));
        p.setProperty("keyShoot", String.valueOf(game.keyShoot));
        p.setProperty("keyBoost", String.valueOf(game.keyBoost));
        p.setProperty("keySlow", String.valueOf(game.keySlow));
        return p;
    }

    void write(Properties p) {
        // Written beside the save first, then swapped in whole, so a crash or
        // power cut mid-write leaves the old save intact instead of half a file.
        // Each write gets its own scratch file, so no two can share one.
        File folder = file.getAbsoluteFile().getParentFile();
        if (folder != null && !folder.isDirectory() && !folder.mkdirs()) return;
        File tmp;
        try { tmp = File.createTempFile(NAME + ".", ".tmp", folder); }
        catch (IOException | SecurityException e) { return; }
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            p.store(out, "RocketLearn progress and settings");
            out.getFD().sync();
        } catch (Exception e) {
            tmp.delete();
            return;
        }
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | UnsupportedOperationException | SecurityException atomicFailed) {
            // Some file systems cannot swap atomically; a plain replace is still
            // safer than writing over the save in place.
            try { Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException | UnsupportedOperationException | SecurityException ignored) { tmp.delete(); }
        }
    }

    static int readInt(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(fallback)).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
