package rocketlearn;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lets a teacher replace the built-in questions with their own, in a plain
 * text file beside the game - no Java and no recompiling.
 *
 * questions.txt is read once at start-up. Any mistake in it means none of it
 * is used: the game keeps the built-in questions, says so on the main menu,
 * and writes every problem with its line number to questions_errors.txt.
 * Half-loading a broken file would quietly drop questions, which is worse.
 *
 * Run `java -cp out rocketlearn.QuestionFile export` to write the built-in
 * questions out in this format as a starting point, and
 * `java -cp out rocketlearn.QuestionFile check` to test a questions.txt
 * without starting the game.
 */
final class QuestionFile {

    static final String FILE_NAME = "questions.txt";
    static final String ERRORS_NAME = "questions_errors.txt";
    static final String EXAMPLE_NAME = "questions.example.txt";

    /** Section names, in tier order. */
    static final String[] TIER_NAMES = { "easy", "medium", "hard" };

    /** Fewer than this in a section and its planets would repeat questions constantly. */
    static final int MIN_PER_TIER = 5;
    static final int WRONG_ANSWERS = 3;

    /** Written in place of Question.BLANK, which is hard to type the right length. */
    static final String BLANK_TOKEN = "[blank]";

    /** A line for the main menu about where the questions came from, or null when there is no file. */
    static String status = null;
    /** True when status reports a problem rather than success. */
    static boolean statusIsProblem = false;

    /**
     * True when the loaded questions.txt says "ORDER: fixed": each section is
     * then asked top to bottom, as written, for lessons that build on each
     * other. Otherwise (and always for the built-in set) they are shuffled.
     */
    static boolean fixedOrder = false;

    /**
     * A topic for a planet, from lines like "FOCUS: Mars = Loops" in the loaded
     * questions.txt: that planet then asks only that topic, so a teacher can
     * make Mars "loops week". Keyed by Planet.ALL index; empty without a file.
     */
    static Map<Integer, String> planetFocus = new HashMap<>();

    /** The questions to play with: questions.txt when it is present and valid, otherwise builtIn. */
    static List<List<Question>> loadTiers(List<List<Question>> builtIn) {
        File file = GameFiles.file(FILE_NAME);
        File errors = GameFiles.file(ERRORS_NAME);
        fixedOrder = false;
        planetFocus = new HashMap<>();
        if (!file.exists()) {
            status = null;
            statusIsProblem = false;
            return builtIn;
        }
        List<String> problems = new ArrayList<>();
        List<List<Question>> tiers = null;
        List<String> lines = null;
        try {
            lines = readLines(file);
            tiers = parse(lines, problems);
        } catch (IOException e) {
            problems.add("Could not read " + FILE_NAME + ": " + e.getMessage());
        }
        if (problems.isEmpty() && tiers != null && lines != null) {
            errors.delete();
            int total = 0;
            for (List<Question> t : tiers) total += t.size();
            fixedOrder = wantsFixedOrder(lines);
            planetFocus = readFocus(lines);
            status = "Questions: " + total + " from " + FILE_NAME + (fixedOrder ? ", asked in order" : "")
                + (planetFocus.isEmpty() ? "" : ", " + planetFocus.size() + " planet focus" + (planetFocus.size() == 1 ? "" : "es"));
            statusIsProblem = false;
            return tiers;
        }
        writeProblems(errors, problems);
        status = FILE_NAME + " has " + problems.size() + " problem" + (problems.size() == 1 ? "" : "s")
            + " - using the built-in questions. See " + ERRORS_NAME;
        statusIsProblem = true;
        return builtIn;
    }

    /** UTF-8 first; a file saved by an older Notepad in the Windows code page still loads. */
    static List<String> readLines(File file) throws IOException {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (CharacterCodingException notUtf8) {
            return Files.readAllLines(file.toPath(), Charset.forName("windows-1252"));
        }
    }

    /** One question as it is read, before it is checked. */
    private static final class Draft {
        final int line, tier;
        String prompt, code, right, why, topic;
        /** As written after TYPE:, lower case, or null for an ordinary four-answer question. */
        String type;
        /** As written after BUG:, or null. */
        String bug;
        final List<String> wrong = new ArrayList<>();
        /** The lines of a CODE: block still being read, until its END CODE. */
        final List<String> openCode = new ArrayList<>();
        Draft(int line, int tier) { this.line = line; this.tier = tier; }

        String label() {
            String p = prompt == null ? "" : prompt;
            return "Question at line " + line + (p.isEmpty() ? "" : " (\"" + (p.length() > 40 ? p.substring(0, 40) + "..." : p) + "\")");
        }
    }

    /** Reads the file's lines into three tiers, adding a message to problems for every mistake. */
    static List<List<Question>> parse(List<String> lines, List<String> problems) {
        List<List<Question>> tiers = new ArrayList<>();
        while (tiers.size() < TIER_NAMES.length) tiers.add(new ArrayList<>());

        int tier = -1;
        Draft cur = null;
        // The question whose CODE: block is being read, or null outside one.
        Draft inCode = null;
        int codeStart = 0;
        // FOCUS: lines seen so far: the line each planet's is on, and each line's topic.
        Map<Integer, Integer> focusLines = new HashMap<>();
        Map<Integer, String> focusTopics = new TreeMap<>();

        for (int i = 0; i < lines.size(); i++) {
            int ln = i + 1;
            String raw = lines.get(i);
            if (i == 0 && raw.startsWith("﻿")) raw = raw.substring(1);   // a byte-order mark from Notepad

            // Inside a code block every line is kept exactly, indentation and all.
            if (inCode != null) {
                if (raw.trim().equalsIgnoreCase("END CODE")) {
                    inCode.code = String.join("\n", inCode.openCode);
                    inCode = null;
                } else {
                    inCode.openCode.add(raw.replace("\t", "    "));
                }
                continue;
            }

            String t = raw.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;

            if (t.startsWith("[") && t.endsWith("]")) {
                finish(cur, tiers, problems);
                cur = null;
                String name = t.substring(1, t.length() - 1).trim().toLowerCase(Locale.ROOT);
                tier = indexOf(name);
                if (tier < 0) problems.add("Line " + ln + ": unknown section " + t + " - use [easy], [medium] or [hard]");
                continue;
            }

            int colon = t.indexOf(':');
            if (colon < 0) {
                problems.add("Line " + ln + ": expected a line like 'Q: ...' (Q, CODE, RIGHT, WRONG, WHY or TOPIC), found \"" + t + "\"");
                continue;
            }
            String key = t.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            String value = t.substring(colon + 1).trim();

            // A planet's topic focus, for the whole file; read by readFocus, checked here.
            if (key.equals("FOCUS")) {
                String[] parts = value.split("=", 2);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    problems.add("Line " + ln + ": FOCUS: must look like 'FOCUS: Mars = Loops'");
                } else {
                    int planet = planetNamed(parts[0]);
                    if (planet < 0) {
                        problems.add("Line " + ln + ": FOCUS: unknown planet \"" + parts[0].trim() + "\" - use one of "
                            + String.join(", ", Planet.ALL.stream().map(p -> p.name).toList()));
                    } else if (focusLines.containsKey(planet)) {
                        problems.add("Line " + ln + ": " + Planet.ALL.get(planet).name + " already has a FOCUS: on line " + focusLines.get(planet));
                    } else {
                        focusLines.put(planet, ln);
                        focusTopics.put(ln, parts[1].trim());
                    }
                }
                continue;
            }

            // A setting for the whole file, not part of any question; read by wantsFixedOrder.
            if (key.equals("ORDER")) {
                String v = value.toLowerCase(Locale.ROOT);
                if (!v.equals("fixed") && !v.equals("shuffled")) {
                    problems.add("Line " + ln + ": ORDER: must be fixed or shuffled, found \"" + value + "\"");
                }
                continue;
            }

            if (key.equals("Q")) {
                finish(cur, tiers, problems);
                if (tier < 0) problems.add("Line " + ln + ": this question is not under an [easy], [medium] or [hard] section");
                cur = new Draft(ln, tier);
                cur.prompt = value;
                continue;
            }
            if (cur == null) {
                problems.add("Line " + ln + ": " + key + ": comes before any Q: line");
                continue;
            }
            switch (key) {
                case "CODE" -> {
                    if (cur.code != null) problems.add("Line " + ln + ": " + cur.label() + " already has a CODE: block");
                    cur.openCode.clear();
                    inCode = cur;
                    codeStart = ln;
                }
                case "RIGHT" -> {
                    if (cur.right != null) problems.add("Line " + ln + ": " + cur.label() + " has more than one RIGHT: answer");
                    cur.right = value;
                }
                case "WRONG" -> cur.wrong.add(value);
                case "WHY" -> cur.why = value;
                case "TOPIC" -> cur.topic = value;
                case "TYPE" -> cur.type = value.toLowerCase(Locale.ROOT);
                case "BUG" -> cur.bug = value;
                default -> problems.add("Line " + ln + ": unknown label " + key + ": - use Q, TYPE, CODE, RIGHT, WRONG, BUG, WHY or TOPIC");
            }
        }
        if (inCode != null) problems.add("Line " + codeStart + ": a CODE: block is never closed with END CODE");
        else finish(cur, tiers, problems);

        for (int i = 0; i < tiers.size(); i++) {
            int n = tiers.get(i).size();
            if (n < MIN_PER_TIER) {
                problems.add("The [" + TIER_NAMES[i] + "] section has " + n + " question" + (n == 1 ? "" : "s")
                    + "; it needs at least " + MIN_PER_TIER);
            }
        }

        // A focus on a topic no question has would leave its planet with nothing to ask.
        Set<String> topics = new HashSet<>();
        for (List<Question> t : tiers) for (Question q : t) topics.add(q.topic);
        for (Map.Entry<Integer, String> f : focusTopics.entrySet()) {
            if (!topics.contains(f.getValue())) {
                problems.add("Line " + f.getKey() + ": FOCUS: no question has TOPIC: " + f.getValue()
                    + " (topic names must match exactly)");
            }
        }
        return tiers;
    }

    /** The Planet.ALL index of a planet by name, ignoring case and spaces around it, or -1. */
    static int planetNamed(String name) {
        for (int i = 0; i < Planet.ALL.size(); i++) if (Planet.ALL.get(i).name.equalsIgnoreCase(name.trim())) return i;
        return -1;
    }

    /** Each planet's FOCUS: topic in a file already checked by parse, keyed by Planet.ALL index. */
    static Map<Integer, String> readFocus(List<String> lines) {
        Map<Integer, String> out = new HashMap<>();
        boolean inCode = false;
        for (String raw : lines) {
            String t = raw.trim();
            if (inCode) { if (t.equalsIgnoreCase("END CODE")) inCode = false; continue; }
            int colon = t.indexOf(':');
            if (colon < 0) continue;
            String key = t.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            if (key.equals("CODE")) inCode = true;
            if (!key.equals("FOCUS")) continue;
            String[] parts = t.substring(colon + 1).split("=", 2);
            if (parts.length != 2) continue;
            int planet = planetNamed(parts[0]);
            if (planet >= 0) out.put(planet, parts[1].trim());
        }
        return out;
    }

    /** Checks a finished question and, if it is complete, adds it to its tier. */
    private static void finish(Draft d, List<List<Question>> tiers, List<String> problems) {
        if (d == null) return;
        int before = problems.size();
        if (d.prompt == null || d.prompt.isEmpty()) problems.add(d.label() + ": the Q: line is empty");
        String type = d.type == null ? TYPE_CHOICE : d.type;
        int bugLine = 0;
        switch (type) {
            case TYPE_CHOICE -> {
                if (d.right == null || d.right.isEmpty()) problems.add(d.label() + ": needs a RIGHT: answer");
                if (d.wrong.size() != WRONG_ANSWERS) {
                    problems.add(d.label() + ": needs exactly " + WRONG_ANSWERS + " WRONG: answers, found " + d.wrong.size());
                }
                for (String w : d.wrong) {
                    if (w.isEmpty()) problems.add(d.label() + ": has an empty WRONG: answer");
                    // Case matters: "String" right and "string" wrong is a real Java lesson.
                    else if (w.equals(d.right)) problems.add(d.label() + ": a WRONG: answer is the same as the RIGHT: one");
                }
                if (d.bug != null) problems.add(d.label() + ": BUG: only belongs in a TYPE: findbug question");
            }
            case TYPE_TRUE_FALSE -> {
                if (d.right == null || !(d.right.equalsIgnoreCase("true") || d.right.equalsIgnoreCase("false"))) {
                    problems.add(d.label() + ": a true/false question needs RIGHT: True or RIGHT: False");
                }
                if (!d.wrong.isEmpty()) problems.add(d.label() + ": a true/false question has no WRONG: lines - the other answer is the wrong one");
                if (d.bug != null) problems.add(d.label() + ": BUG: only belongs in a TYPE: findbug question");
            }
            case TYPE_FIND_BUG -> {
                int lines = d.code == null ? 0 : d.code.split("\n").length;
                if (lines < 2) problems.add(d.label() + ": a find-the-bug question needs a CODE: block of at least 2 lines");
                try {
                    bugLine = Integer.parseInt(d.bug == null ? "" : d.bug.trim());
                    if (lines >= 2 && (bugLine < 1 || bugLine > lines)) {
                        problems.add(d.label() + ": BUG: " + bugLine + " is not a line of its code (1 to " + lines + ")");
                    }
                } catch (NumberFormatException e) {
                    problems.add(d.label() + ": a find-the-bug question needs BUG: and the number of the wrong line");
                }
                if (d.right != null || !d.wrong.isEmpty()) {
                    problems.add(d.label() + ": a find-the-bug question uses BUG: instead of RIGHT: and WRONG: - the lines are the answers");
                }
            }
            default -> problems.add(d.label() + ": unknown TYPE: " + d.type + " - use truefalse or findbug, or leave TYPE: out");
        }
        if (d.why == null || d.why.isEmpty()) problems.add(d.label() + ": needs a WHY: explanation, shown when a player gets it wrong");
        if (d.topic == null || d.topic.isEmpty()) problems.add(d.label() + ": needs a TOPIC:");
        if (problems.size() > before || d.tier < 0) return;

        String prompt = fillBlank(d.prompt), code = d.code == null ? null : fillBlank(d.code);
        Question q = switch (type) {
            case TYPE_TRUE_FALSE -> Question.trueFalse(prompt, code, d.right.equalsIgnoreCase("true"), d.why, d.topic);
            case TYPE_FIND_BUG -> Question.findBug(prompt, code, bugLine, d.why, d.topic);
            default -> {
                String[] choices = new String[1 + d.wrong.size()];
                choices[0] = d.right;
                for (int i = 0; i < d.wrong.size(); i++) choices[i + 1] = d.wrong.get(i);
                yield new Question(prompt, code, choices, 0, d.why, d.topic);
            }
        };
        tiers.get(d.tier).add(q);
    }

    /** What TYPE: may say. Without one, a question is an ordinary four-answer one. */
    static final String TYPE_CHOICE = "choice", TYPE_TRUE_FALSE = "truefalse", TYPE_FIND_BUG = "findbug";

    /** Whether the file asks for its questions in the order written: the last ORDER: line outside code wins. */
    static boolean wantsFixedOrder(List<String> lines) {
        boolean fixed = false, inCode = false;
        for (String raw : lines) {
            String t = raw.trim();
            if (inCode) { if (t.equalsIgnoreCase("END CODE")) inCode = false; continue; }
            int colon = t.indexOf(':');
            if (colon < 0) continue;
            String key = t.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            if (key.equals("CODE")) inCode = true;
            else if (key.equals("ORDER")) fixed = t.substring(colon + 1).trim().equalsIgnoreCase("fixed");
        }
        return fixed;
    }

    static String fillBlank(String s) {
        return s.replaceAll("(?i)" + java.util.regex.Pattern.quote(BLANK_TOKEN), Question.BLANK);
    }

    static int indexOf(String tierName) {
        for (int i = 0; i < TIER_NAMES.length; i++) if (TIER_NAMES[i].equals(tierName)) return i;
        return -1;
    }

    /** The questions in this file's format, with a header explaining it. */
    static String format(List<List<Question>> tiers) {
        StringBuilder sb = new StringBuilder(header());
        for (int t = 0; t < tiers.size(); t++) {
            sb.append("\n[").append(TIER_NAMES[t]).append("]\n");
            for (Question q : tiers.get(t)) appendQuestion(sb, q);
        }
        return sb.toString();
    }

    /** The notes at the top of a questions file: how to write one. */
    static String header() {
        StringBuilder sb = new StringBuilder();
        sb.append("# RocketLearn questions\n")
          .append("#\n")
          .append("# Save a copy of this file as questions.txt beside the game to use your own\n")
          .append("# questions. Lines starting with # are notes and are ignored.\n")
          .append("#\n")
          .append("# [easy], [medium] and [hard] start a section; each needs at least ").append(MIN_PER_TIER).append(" questions.\n")
          .append("# Each question is:\n")
          .append("#   Q:      the question\n")
          .append("#   CODE:   optional - code lines follow, ending with a line that says END CODE.\n")
          .append("#           Write ").append(BLANK_TOKEN).append(" where the answer goes.\n")
          .append("#   RIGHT:  the correct answer\n")
          .append("#   WRONG:  a wrong answer - exactly ").append(WRONG_ANSWERS).append(" of these\n")
          .append("#   WHY:    the explanation shown when a player gets it wrong\n")
          .append("#   TOPIC:  the subject, used for the Topic Stats screen and practice\n")
          .append("#\n")
          .append("# Two other kinds of question, chosen with a TYPE: line after Q:\n")
          .append("#   TYPE: truefalse   two asteroids, True and False. Give RIGHT: True or\n")
          .append("#                     RIGHT: False, and no WRONG: lines.\n")
          .append("#   TYPE: findbug     a CODE: block shown with line numbers, and BUG: the\n")
          .append("#                     number of the wrong line. The asteroids offer that line\n")
          .append("#                     and its neighbours; no RIGHT: or WRONG: lines.\n")
          .append("#\n")
          .append("# Questions are shuffled. To ask each section top to bottom as written, for\n")
          .append("# lessons that build on each other, add this line near the top:\n")
          .append("#   ORDER: fixed\n")
          .append("#\n")
          .append("# To make a planet ask only one topic (say, Mars for loops week), add a line\n")
          .append("# like this near the top - the topic must match a TOPIC: exactly:\n")
          .append("#   FOCUS: Mars = Loops\n")
          .append("#\n")
          .append("# If anything is wrong the game uses its built-in questions instead and lists\n")
          .append("# the problems, with line numbers, in ").append(ERRORS_NAME).append(".\n");
        return sb.toString();
    }

    /** One question, as a blank line and its labelled lines. */
    static void appendQuestion(StringBuilder sb, Question q) {
        sb.append("\nQ: ").append(unfill(q.prompt)).append('\n');
        if (q.kind == Question.Kind.TRUE_FALSE) sb.append("TYPE: ").append(TYPE_TRUE_FALSE).append('\n');
        if (q.kind == Question.Kind.FIND_BUG) sb.append("TYPE: ").append(TYPE_FIND_BUG).append('\n');
        if (q.hasCode()) sb.append("CODE:\n").append(unfill(q.code)).append("\nEND CODE\n");
        if (q.kind == Question.Kind.FIND_BUG) {
            sb.append("BUG: ").append(q.bugLine).append('\n');
        } else {
            sb.append("RIGHT: ").append(q.choices[q.correct]).append('\n');
            if (q.kind == Question.Kind.CHOICE) {
                for (int i = 0; i < q.choices.length; i++) if (i != q.correct) sb.append("WRONG: ").append(q.choices[i]).append('\n');
            }
        }
        sb.append("WHY: ").append(q.why).append('\n');
        sb.append("TOPIC: ").append(q.topic).append('\n');
    }

    static String unfill(String s) {
        return s.replace(Question.BLANK, BLANK_TOKEN);
    }

    static void writeProblems(File errors, List<String> problems) {
        StringBuilder sb = new StringBuilder();
        sb.append("RocketLearn could not use ").append(FILE_NAME).append(", so it is playing with its built-in questions.\n")
          .append("Fix these and restart the game:\n\n");
        for (String p : problems) sb.append("- ").append(p).append('\n');
        try { Files.writeString(errors.toPath(), sb.toString(), StandardCharsets.UTF_8); }
        catch (IOException ignored) { }
    }

    /** export [file]: writes the built-in questions out.  check [file]: reports problems in a questions file. */
    public static void main(String[] args) throws IOException {
        String cmd = args.length > 0 ? args[0] : "";
        switch (cmd) {
            case "export" -> {
                File out = new File(args.length > 1 ? args[1] : EXAMPLE_NAME);
                Files.writeString(out.toPath(), format(QuestionBank.BUILT_IN_TIERS), StandardCharsets.UTF_8);
                System.out.println("Wrote the built-in questions to " + out);
            }
            case "check" -> {
                File in = new File(args.length > 1 ? args[1] : FILE_NAME);
                if (!in.exists()) {
                    System.out.println("There is no " + in + " here. Copy " + EXAMPLE_NAME + " to " + FILE_NAME + " to start one.");
                    System.exit(1);
                }
                List<String> problems = new ArrayList<>();
                List<String> lines = readLines(in);
                List<List<Question>> tiers = parse(lines, problems);
                if (problems.isEmpty()) {
                    Map<Integer, String> focus = readFocus(lines);
                    System.out.println(in + " is fine: " + tiers.get(0).size() + " easy, " + tiers.get(1).size()
                        + " medium, " + tiers.get(2).size() + " hard, " + (wantsFixedOrder(lines) ? "asked in order" : "shuffled")
                        + (focus.isEmpty() ? "." : ", with a topic focus on " + focus.size() + " planet" + (focus.size() == 1 ? "." : "s.")));
                } else {
                    System.out.println(in + " has " + problems.size() + " problem(s):");
                    for (String p : problems) System.out.println("  - " + p);
                    System.exit(1);
                }
            }
            default -> {
                System.out.println("Usage: java -cp out rocketlearn.QuestionFile export [file]   (default " + EXAMPLE_NAME + ")");
                System.out.println("       java -cp out rocketlearn.QuestionFile check [file]    (default " + FILE_NAME + ")");
            }
        }
    }

    private QuestionFile() { }
}
