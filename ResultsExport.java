package rocketlearn;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A player's results as a spreadsheet a teacher can collect: accuracy per
 * topic, each planet finished and the medal earned there, and every question
 * in the notebook with its right answer and whether it has since been
 * cleared, or only got right on a retry.
 *
 * One file, one table, so it opens straight into Excel or Google Sheets and a
 * class's files can be pasted one under another. The Section column tells the
 * topic rows from the question rows. There is no player name in the game, so
 * the computer's login name stands in for one, which on a school network is
 * usually the student's own.
 */
final class ResultsExport {

    static final String[] HEADER = {
        "Student", "Exported", "Section", "Topic",
        "Answered", "Right first try", "Accuracy %", "Still to clear",
        "Question", "Correct answer", "Cleared", "Medal", "Version"
    };

    /** Writes the results, without the planet rows. Kept for callers that have no game to hand. */
    static File export(Stats stats, Notebook notebook, String student, LocalDateTime when) throws IOException {
        return export(stats, notebook, null, 1, student, when);
    }

    /** Writes the results beside the save and returns the file written. */
    static File export(Stats stats, Notebook notebook, PlanetRecords planets, int highestLevelReached,
                       String student, LocalDateTime when) throws IOException {
        File folder = SaveFile.SAVE_FILE.getAbsoluteFile().getParentFile();
        if (folder != null && !folder.isDirectory() && !folder.mkdirs()) throw new IOException("cannot create " + folder);
        String stamp = when.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"));
        File out = new File(folder, FILE_PREFIX + safeName(student) + " - " + stamp + ".csv");
        // A byte-order mark, or Excel reads the file as the old Windows code
        // page and garbles any character outside plain ASCII.
        Files.writeString(out.toPath(), "﻿" + toCsv(stats, notebook, planets, highestLevelReached, student, when), StandardCharsets.UTF_8);
        return out;
    }

    static String toCsv(Stats stats, Notebook notebook, String student, LocalDateTime when) {
        return toCsv(stats, notebook, null, 1, student, when);
    }

    /**
     * The table. With planet records it also has one Planet row per planet in
     * route order: finished or not, and the best medal, so a teacher can see
     * at a glance how far through the journey each student has got.
     */
    static String toCsv(Stats stats, Notebook notebook, PlanetRecords planets, int highestLevelReached,
                        String student, LocalDateTime when) {
        String exported = when.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        List<String> rows = new ArrayList<>();
        rows.add(row(HEADER));

        List<Question> toClear = notebook.toClear();
        for (String topic : RocketLearn.ALL_TOPICS) {
            int[] t = stats.byTopic.get(topic);
            int answered = t == null ? 0 : t[1], right = t == null ? 0 : t[0];
            long left = toClear.stream().filter(q -> q.topic.equals(topic)).count();
            rows.add(row(student, exported, "Topic", topic,
                String.valueOf(answered), String.valueOf(right),
                answered == 0 ? "" : String.valueOf(Math.round(right * 100.0 / answered)),
                String.valueOf(left), "", "", "", ""));
        }
        // Topics only a teacher's own questions.txt uses are not in ALL_TOPICS.
        for (Map.Entry<String, int[]> e : stats.byTopic.entrySet()) {
            if (RocketLearn.ALL_TOPICS.contains(e.getKey())) continue;
            int[] t = e.getValue();
            rows.add(row(student, exported, "Topic", e.getKey(), String.valueOf(t[1]), String.valueOf(t[0]),
                t[1] == 0 ? "" : String.valueOf(Math.round(t[0] * 100.0 / t[1])), "", "", "", "", ""));
        }
        rows.add(row(student, exported, "All topics", "",
            String.valueOf(stats.answered), String.valueOf(stats.correct),
            stats.answered == 0 ? "" : String.valueOf(stats.accuracyPercent()),
            String.valueOf(toClear.size()), "", "", "", ""));

        if (planets != null) {
            for (int p : RocketLearn.TRAVEL_ORDER) {
                boolean done = planets.finished(p, highestLevelReached);
                PlanetRecords.Medal medal = planets.shown(p, highestLevelReached);
                rows.add(row(student, exported, "Planet", Planet.ALL.get(p).name, "", "", "", "", "", "",
                    done ? "Yes" : "No", done ? medal.label : ""));
            }
        }

        for (Question q : notebook.entries) {
            rows.add(row(student, exported, "Missed question", q.topic, "", "", "", "",
                q.prompt + (q.hasCode() ? "\n" + String.join("\n", q.codeLines()) : ""),
                q.correctAnswer(), notebook.isCleared(q) ? "Yes" : notebook.isRecovered(q) ? "Partly - right on a retry" : "No", ""));
        }
        // Which build made the file, on every row, so it survives a class's files being pasted together.
        for (int i = 1; i < rows.size(); i++) rows.set(i, rows.get(i) + "," + cell(RocketLearn.VERSION));
        return String.join("\r\n", rows) + "\r\n";
    }

    private static String row(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(fields[i]));
        }
        return sb.toString();
    }

    /**
     * One CSV cell: quoted when it holds a comma, quote or line break, with
     * quotes doubled. A cell starting with = + - or @ gets a leading quote
     * mark, so a spreadsheet shows it as text instead of running it as a formula.
     */
    static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        boolean quote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        return quote ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    // ---- the class summary ------------------------------------------------

    /** What every student's export file is named like; the summary itself is not one. */
    static final String FILE_PREFIX = "RocketLearn results - ";
    static final String SUMMARY_PREFIX = "RocketLearn class summary - ";
    /** A planet not finished yet, in the summary. Words rather than "-", which a spreadsheet would take for a minus sign. */
    static final String NOT_YET = "not yet";

    /**
     * Reads every student export in a folder and writes one summary beside
     * them: a row per student with their accuracy, weakest topic, what they
     * still have to clear and their medal on every planet. Columns are found
     * by name, so files from older versions of the game (without the Medal or
     * Version column) still count. When a student has exported more than
     * once, the newest file wins. Returns the file written.
     */
    static File mergeClass(File folder, LocalDateTime when) throws IOException {
        File[] files = folder.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) throw new IOException("no RocketLearn results files in " + folder);
        Map<String, String[]> byStudent = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> exportedAt = new java.util.HashMap<>();
        for (File f : files) {
            String[] summary = summarise(parseCsv(Files.readString(f.toPath(), StandardCharsets.UTF_8)));
            if (summary == null) continue;
            String student = summary[0], exported = summary[1];
            if (!byStudent.containsKey(student) || exported.compareTo(exportedAt.get(student)) > 0) {
                byStudent.put(student, summary);
                exportedAt.put(student, exported);
            }
        }
        if (byStudent.isEmpty()) throw new IOException("none of the results files in " + folder + " could be read");

        List<String> header = new ArrayList<>(List.of("Student", "Exported", "Version", "Answered", "Right first try",
            "Accuracy %", "Weakest topic", "Still to clear", "Planets finished", "Gold medals"));
        for (int p : RocketLearn.TRAVEL_ORDER) header.add(Planet.ALL.get(p).name);
        List<String> rows = new ArrayList<>();
        rows.add(row(header.toArray(String[]::new)));
        for (String[] s : byStudent.values()) rows.add(row(s));

        String stamp = when.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"));
        File out = new File(folder, SUMMARY_PREFIX + stamp + ".csv");
        Files.writeString(out.toPath(), "﻿" + String.join("\r\n", rows) + "\r\n", StandardCharsets.UTF_8);
        return out;
    }

    /** One student's export boiled down to a summary row, or null if the file is not a results export. */
    static String[] summarise(List<List<String>> table) {
        if (table.isEmpty()) return null;
        List<String> head = table.get(0);
        int student = head.indexOf("Student"), exported = head.indexOf("Exported"), section = head.indexOf("Section"),
            topic = head.indexOf("Topic"), answered = head.indexOf("Answered"), right = head.indexOf("Right first try"),
            accuracy = head.indexOf("Accuracy %"), toClear = head.indexOf("Still to clear"),
            cleared = head.indexOf("Cleared"), medal = head.indexOf("Medal"), version = head.indexOf("Version");
        if (student < 0 || section < 0 || topic < 0) return null;

        String name = "", when = "", ver = "", all = "", allRight = "", allPct = "", left = "", weakest = "";
        int weakestPct = 101;
        Map<String, String> medals = new java.util.HashMap<>();
        for (List<String> r : table.subList(1, table.size())) {
            if (r.size() <= Math.max(section, topic)) continue;
            name = get(r, student);
            when = get(r, exported);
            if (version >= 0) ver = get(r, version);
            switch (get(r, section)) {
                case "Topic" -> {
                    int n = parseInt(get(r, answered)), pct = parseInt(get(r, accuracy));
                    if (n >= Stats.MIN_SAMPLES && pct < weakestPct) { weakestPct = pct; weakest = get(r, topic) + " (" + pct + "%)"; }
                }
                case "All topics" -> {
                    all = get(r, answered);
                    allRight = get(r, right);
                    allPct = get(r, accuracy);
                    left = get(r, toClear);
                }
                case "Planet" -> {
                    boolean done = "Yes".equals(get(r, cleared));
                    medals.put(get(r, topic), !done ? NOT_YET : medal >= 0 && !get(r, medal).isEmpty() ? get(r, medal) : "Finished");
                }
                default -> { }
            }
        }
        if (name.isEmpty()) return null;
        long finished = medals.values().stream().filter(m -> !m.equals(NOT_YET)).count();
        long gold = medals.values().stream().filter(m -> m.equals("Gold")).count();
        List<String> out = new ArrayList<>(List.of(name, when, ver.isEmpty() ? "before 1.1" : ver, all, allRight, allPct,
            weakest.isEmpty() ? "not enough answers yet" : weakest, left,
            medals.isEmpty() ? "" : String.valueOf(finished), medals.isEmpty() ? "" : String.valueOf(gold)));
        for (int p : RocketLearn.TRAVEL_ORDER) out.add(medals.getOrDefault(Planet.ALL.get(p).name, medals.isEmpty() ? "" : NOT_YET));
        return out.toArray(String[]::new);
    }

    private static String get(List<String> row, int col) {
        if (col < 0 || col >= row.size()) return "";
        String v = row.get(col);
        // Undo the guard cell() puts on text that looks like a formula.
        return v.length() > 1 && v.charAt(0) == '\'' && "=+-@".indexOf(v.charAt(1)) >= 0 ? v.substring(1) : v;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }

    /**
     * Reads CSV as this class writes it: comma separated, cells with commas,
     * quotes or line breaks wrapped in quotes with quotes doubled, and an
     * optional byte-order mark at the start.
     */
    static List<List<String>> parseCsv(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cell.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString());
                cell.setLength(0);
                table.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); table.add(row); }
        return table;
    }

    /** merge [folder]: writes the class summary for the results files in a folder (default: this one). */
    public static void main(String[] args) {
        if (args.length == 0 || !args[0].equals("merge")) {
            System.out.println("Usage: java -cp RocketLearn.jar rocketlearn.ResultsExport merge [folder]");
            System.out.println("       Combines every '" + FILE_PREFIX + "...csv' in the folder into one class summary.");
            return;
        }
        File folder = new File(args.length > 1 ? args[1] : ".");
        try {
            File out = mergeClass(folder, LocalDateTime.now());
            System.out.println("Wrote " + out.getName() + " in " + folder.getAbsoluteFile());
        } catch (IOException e) {
            System.out.println("Could not make the class summary: " + e.getMessage());
            System.exit(1);
        }
    }

    /** A login name made safe to use in a file name. */
    static String safeName(String name) {
        String s = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return s.isEmpty() ? "player" : s;
    }

    /** The name the export is filed under. */
    static String studentName() {
        // The chosen pilot is the student; with no pilots, the computer's login stands in.
        String pilot = SaveFile.pilotName;
        if (pilot != null && !pilot.isBlank()) return pilot;
        return System.getProperty("user.name", "player");
    }

    private ResultsExport() { }
}
