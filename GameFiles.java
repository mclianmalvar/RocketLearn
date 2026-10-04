package rocketlearn;

import java.io.File;

/**
 * Where the game's own files live: the save, the music, and questions.txt.
 *
 * Normally that is the folder the game was started from. Run from a jar, it
 * is the jar's own folder instead, so double-clicking RocketLearn.jar finds
 * its music and keeps its save beside it however Windows chose to launch it.
 * Started from a folder with no music but a folder inside it that has some,
 * it uses that inner folder. -Drocketlearn.home=<folder> overrides all of this.
 */
final class GameFiles {

    private static File dir;

    static synchronized File dir() {
        if (dir == null) dir = findDir();
        return dir;
    }

    static File file(String name) {
        return new File(dir(), name);
    }

    private static File findDir() {
        String home = System.getProperty("rocketlearn.home");
        if (home != null && !home.isEmpty()) return new File(home);
        try {
            java.security.CodeSource source = GameFiles.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                File code = new File(source.getLocation().toURI());
                if (code.isFile() && code.getName().toLowerCase().endsWith(".jar")) return code.getAbsoluteFile().getParentFile();
            }
        } catch (java.net.URISyntaxException | IllegalArgumentException | SecurityException ignored) {
            // No readable code location: fall back to the working folder.
        }
        File here = new File(".");
        if (holdsMusic(here)) return here;
        // An editor opened on the folder around the game starts it one level
        // too high, where none of the music is, and every track would fall
        // back to its generated loop. Use the folder inside that has it.
        File[] inside = here.listFiles(File::isDirectory);
        if (inside != null) {
            for (File sub : inside) {
                if (holdsMusic(sub)) return sub;
            }
        }
        return here;
    }

    /** Whether a folder has any of the game's music files in it. */
    private static boolean holdsMusic(File folder) {
        for (String name : MUSIC_FILES) {
            if (new File(folder, name).isFile()) return true;
        }
        return false;
    }

    private static final String[] MUSIC_FILES = {
        "menu_music.wav", "easy_music.wav", "medium_music.wav", "hard_music.wav", "boss_music.wav"
    };

    private GameFiles() { }
}
