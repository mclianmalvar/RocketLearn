package rocketlearn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One multiple-choice question, its correct answer, the explanation shown when
 * the player gets it wrong, and the topic it belongs to.
 *
 * A question may also carry a code snippet. When it does, the prompt asks what
 * belongs in the snippet's blank and the snippet is drawn under it in a
 * monospace block, so a fill-in-the-blank reads as code rather than as prose.
 */
class Question {

    /** Marks the gap in a code snippet. Wide enough to read as a slot. */
    static final String BLANK = "______";

    /**
     * The shape of the question. Every kind plays the same way - shoot the
     * right asteroid - and differs only in what the asteroids say.
     */
    enum Kind {
        /** Four answers, one right. */
        CHOICE,
        /** Two asteroids, True and False. */
        TRUE_FALSE,
        /** A numbered snippet; the asteroids name lines and the right one holds the bug. */
        FIND_BUG
    }

    final String prompt;
    final String[] choices;
    final int correct;
    final String why;
    final String topic;
    final Kind kind;
    /** For FIND_BUG, the line (counting from 1) that holds the bug; 0 otherwise. */
    final int bugLine;

    /** Lines of code shown beneath the prompt, newline separated, or null. */
    final String code;

    Question(String prompt, String[] choices, int correct, String why, String topic) {
        this(prompt, null, choices, correct, why, topic);
    }

    Question(String prompt, String code, String[] choices, int correct, String why, String topic) {
        this(prompt, code, choices, correct, why, topic, Kind.CHOICE, 0);
    }

    private Question(String prompt, String code, String[] choices, int correct, String why, String topic,
                     Kind kind, int bugLine) {
        this.prompt = prompt;
        this.code = code;
        this.choices = choices;
        this.correct = correct;
        this.why = why;
        this.topic = topic;
        this.kind = kind;
        this.bugLine = bugLine;
    }

    /** A statement to judge: the asteroids read True and False. */
    static Question trueFalse(String prompt, String code, boolean answer, String why, String topic) {
        return new Question(prompt, code, new String[]{ "True", "False" }, answer ? 0 : 1, why, topic, Kind.TRUE_FALSE, 0);
    }

    /** Most lines a find-the-bug question offers as answers. */
    static final int BUG_CHOICES = 4;

    /**
     * A snippet with one wrong line. The asteroids offer the buggy line and the
     * lines nearest it - up to four in all, listed in order - so the choice is
     * between neighbours rather than obvious outliers.
     */
    static Question findBug(String prompt, String code, int bugLine, String why, String topic) {
        int lines = code.split("\n").length;
        List<Integer> picked = new ArrayList<>();
        picked.add(bugLine);
        for (int d = 1; picked.size() < Math.min(BUG_CHOICES, lines); d++) {
            if (bugLine - d >= 1) picked.add(bugLine - d);
            if (picked.size() < Math.min(BUG_CHOICES, lines) && bugLine + d <= lines) picked.add(bugLine + d);
        }
        Collections.sort(picked);
        String[] choices = new String[picked.size()];
        for (int i = 0; i < choices.length; i++) choices[i] = "Line " + picked.get(i);
        return new Question(prompt, code, choices, picked.indexOf(bugLine), why, topic, Kind.FIND_BUG, bugLine);
    }

    boolean hasCode() {
        return code != null && !code.isEmpty();
    }

    /** The snippet as shown. A find-the-bug snippet is numbered, since its answers name lines. */
    String[] codeLines() {
        if (!hasCode()) return new String[0];
        String[] lines = code.split("\n");
        if (kind == Kind.FIND_BUG) {
            int width = String.valueOf(lines.length).length();
            for (int i = 0; i < lines.length; i++) lines[i] = String.format("%" + width + "d | %s", i + 1, lines[i]);
        }
        return lines;
    }

    String correctAnswer() {
        return choices[correct];
    }
}
