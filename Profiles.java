package rocketlearn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The pilots who play on this computer, each with their own save, so a class
 * sharing a computer lab never plays on each other's progress.
 *
 * The list lives in rocketlearn.profiles beside the save. The first pilot
 * keeps rocketlearn.save, so a game played before profiles existed simply
 * becomes "Player 1"; every other pilot gets rocketlearn-name.save. A
 * removed pilot's save is renamed, never deleted, so it can be got back.
 */
final class Profiles {

    static final String FILE_NAME = "rocketlearn.profiles";
    static final String FIRST_PILOT = "Player 1";
    static final int MAX_NAME = 14;

    record Pilot(String name, String fileName) { }

    final File folder;
    final List<Pilot> pilots = new ArrayList<>();
    /** The pilot chosen last time, or null. */
    String last;

    Profiles(File folder) {
        this.folder = folder;
        load();
    }

    /** The profiles beside the default save. */
    static Profiles here() { return new Profiles(SaveFile.DEFAULT_SAVE.getAbsoluteFile().getParentFile()); }

    File listFile() { return new File(folder, FILE_NAME); }

    File saveOf(Pilot p) { return new File(folder, p.fileName()); }

    void load() {
        pilots.clear();
        last = null;
        File f = listFile();
        if (f.isFile()) {
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (IOException | IllegalArgumentException e) {
                p.clear();
            }
            for (int i = 0; ; i++) {
                String name = p.getProperty("pilot." + i + ".name"), file = p.getProperty("pilot." + i + ".file");
                if (name == null || file == null) break;
                name = cleanName(name);
                // Only a plain save file name in this folder: a hand-edited list cannot point elsewhere.
                if (!name.isEmpty() && find(name) == null && file.matches("rocketlearn(-[A-Za-z0-9_-]+)?\\.save")) {
                    pilots.add(new Pilot(name, file));
                }
            }
            last = p.getProperty("last");
        }
        // A save made before profiles existed belongs to the first pilot.
        if (pilots.isEmpty() && new File(folder, SaveFile.NAME).isFile()) pilots.add(new Pilot(FIRST_PILOT, SaveFile.NAME));
        if (find(last) == null) last = pilots.isEmpty() ? null : pilots.get(0).name();
    }

    void save() throws IOException {
        Properties p = new Properties();
        for (int i = 0; i < pilots.size(); i++) {
            p.setProperty("pilot." + i + ".name", pilots.get(i).name());
            p.setProperty("pilot." + i + ".file", pilots.get(i).fileName());
        }
        if (last != null) p.setProperty("last", last);
        try (FileOutputStream out = new FileOutputStream(listFile())) {
            p.store(out, "RocketLearn pilots on this computer");
        }
    }

    Pilot find(String name) {
        if (name == null) return null;
        for (Pilot p : pilots) if (p.name().equalsIgnoreCase(name.trim())) return p;
        return null;
    }

    /** The pilot to start as: the last one chosen, the only one, or null when there are none. */
    Pilot lastPilot() { return find(last); }

    /** Trims a typed name and keeps only letters, digits, spaces and . ' - characters. */
    static String cleanName(String raw) {
        StringBuilder sb = new StringBuilder();
        for (char c : (raw == null ? "" : raw).toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '.' || c == '\'' || c == '-') sb.append(c);
        }
        String s = sb.toString().trim().replaceAll(" +", " ");
        return s.length() > MAX_NAME ? s.substring(0, MAX_NAME).trim() : s;
    }

    /** Why a name cannot be used, or null when it can. */
    String problemWith(String name, Pilot except) {
        String n = cleanName(name);
        if (n.isEmpty()) return "Type a name first";
        Pilot same = find(n);
        if (same != null && same != except) return n + " is already a pilot here";
        return null;
    }

    /** Adds a pilot and returns it. The first pilot takes the original save file. */
    Pilot add(String rawName) throws IOException {
        String name = cleanName(rawName);
        if (problemWith(name, null) != null) throw new IOException(problemWith(name, null));
        String file;
        if (pilots.isEmpty()) {
            file = SaveFile.NAME;
        } else {
            String base = "rocketlearn-" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            if (base.equals("rocketlearn-")) base = "rocketlearn-pilot";
            file = base + ".save";
            for (int n = 2; takenFile(file); n++) file = base + "-" + n + ".save";
        }
        Pilot p = new Pilot(name, file);
        pilots.add(p);
        save();
        return p;
    }

    private boolean takenFile(String file) {
        if (new File(folder, file).exists()) return true;
        for (Pilot p : pilots) if (p.fileName().equalsIgnoreCase(file)) return true;
        return false;
    }

    void rename(Pilot p, String rawName) throws IOException {
        String name = cleanName(rawName);
        if (problemWith(name, p) != null) throw new IOException(problemWith(name, p));
        int i = pilots.indexOf(p);
        if (i < 0) return;
        Pilot renamed = new Pilot(name, p.fileName());
        pilots.set(i, renamed);
        if (p.name().equalsIgnoreCase(last == null ? "" : last)) last = name;
        save();
    }

    /** Takes a pilot off the list. Their save is renamed to .removed, so it is not lost. */
    void remove(Pilot p) throws IOException {
        if (!pilots.remove(p)) return;
        File save = saveOf(p);
        if (save.exists()) {
            File kept = new File(folder, p.fileName() + ".removed");
            for (int n = 2; kept.exists(); n++) kept = new File(folder, p.fileName() + ".removed" + n);
            if (!save.renameTo(kept)) throw new IOException("could not set aside " + save.getName());
        }
        if (p.name().equalsIgnoreCase(last == null ? "" : last)) last = pilots.isEmpty() ? null : pilots.get(0).name();
        save();
    }

    void choose(Pilot p) throws IOException {
        last = p.name();
        save();
    }
}
