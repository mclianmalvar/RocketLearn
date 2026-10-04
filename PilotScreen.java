package rocketlearn;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import static rocketlearn.RocketLearn.*;

/**
 * "Who's playing?": pick your pilot, add one, rename or remove one. Shown at
 * launch when more than one pilot shares the computer, and from the main
 * menu (P, or the pilot label in the corner) at any time.
 */
class PilotScreen {

    final RocketLearn game;

    PilotScreen(RocketLearn game) { this.game = game; }

    enum Typing { NONE, NEW, RENAME }

    Profiles profiles;
    int index;
    Typing typing = Typing.NONE;
    final StringBuilder buffer = new StringBuilder();
    /** A line under the list: what went wrong, or what the next press will do. */
    String message;
    boolean removeArmed;

    static final int LIST_TOP = 150, ROW_GAP = 38;

    /** The pilot label on the main menu, in game coordinates; a click there opens this screen. */
    static final Rectangle2D LABEL = new Rectangle2D.Double(GAME_WIDTH - 250, 6, 240, 22);

    /** Opens the screen on the pilot playing now. */
    void open() {
        profiles = Profiles.here();
        Profiles.Pilot now = profiles.find(SaveFile.pilotName);
        index = now == null ? 0 : profiles.pilots.indexOf(now);
        typing = Typing.NONE;
        buffer.setLength(0);
        message = null;
        removeArmed = false;
        game.state = State.PILOTS;
    }

    /** The pilot list, read when the screen is first needed - also when it is shown without open(). */
    Profiles profiles() {
        if (profiles == null) profiles = Profiles.here();
        return profiles;
    }

    int rows() { return profiles().pilots.size() + 2; }
    int newRow() { return profiles().pilots.size(); }
    int backRow() { return profiles().pilots.size() + 1; }

    Profiles.Pilot selectedPilot() {
        profiles();
        return index >= 0 && index < profiles.pilots.size() ? profiles.pilots.get(index) : null;
    }

    void select(int i) {
        if (i == index) return;
        index = clampInt(i, 0, rows() - 1);
        removeArmed = false;
        message = null;
    }

    /** Enter, a click or the pad's A on the selected row. */
    void confirm() {
        if (typing != Typing.NONE) { finishTyping(); return; }
        if (index == newRow()) {
            startTyping(Typing.NEW, "");
        } else if (index == backRow()) {
            game.playSfx(Sfx.CONFIRM);
            game.state = State.MENU;
        } else {
            choose(selectedPilot());
        }
    }

    /** Becomes this pilot: their own save, progress and settings. */
    void choose(Profiles.Pilot p) {
        if (p == null) return;
        game.playSfx(Sfx.CONFIRM);
        try {
            profiles.choose(p);
        } catch (IOException e) {
            message = "Could not remember the choice: " + e.getMessage();
        }
        boolean already = game.saveFile.file.getAbsoluteFile().equals(profiles.saveOf(p).getAbsoluteFile());
        SaveFile.pilotName = p.name();
        if (already) {
            game.state = State.MENU;
            return;
        }
        SaveFile.SAVE_FILE = profiles.saveOf(p);
        game.reborn();
    }

    void startTyping(Typing mode, String start) {
        typing = mode;
        buffer.setLength(0);
        buffer.append(start);
        message = null;
        removeArmed = false;
        game.playSfx(Sfx.CLICK);
    }

    void finishTyping() {
        String name = Profiles.cleanName(buffer.toString());
        Profiles.Pilot renaming = typing == Typing.RENAME ? selectedPilot() : null;
        String problem = profiles.problemWith(name, renaming);
        if (problem != null) {
            message = problem;
            game.playSfx(Sfx.CLICK);
            return;
        }
        try {
            if (typing == Typing.NEW) {
                typing = Typing.NONE;
                Profiles.Pilot p = profiles.add(name);
                index = profiles.pilots.indexOf(p);
                choose(p);
            } else {
                boolean wasPlaying = renaming.name().equalsIgnoreCase(SaveFile.pilotName == null ? "" : SaveFile.pilotName);
                profiles.rename(renaming, name);
                if (wasPlaying) SaveFile.pilotName = name;
                typing = Typing.NONE;
                game.playSfx(Sfx.CONFIRM);
                message = "Renamed to " + name;
            }
        } catch (IOException e) {
            message = e.getMessage();
        }
    }

    /** Every key on this screen. Returns true when the key was used. */
    boolean keyPressed(int k, char ch) {
        if (typing != Typing.NONE) {
            if (k == KeyEvent.VK_ENTER) finishTyping();
            else if (k == KeyEvent.VK_ESCAPE) { typing = Typing.NONE; message = null; game.playSfx(Sfx.CLICK); }
            else if (k == KeyEvent.VK_BACK_SPACE) { if (buffer.length() > 0) buffer.setLength(buffer.length() - 1); }
            else if (ch != KeyEvent.CHAR_UNDEFINED && buffer.length() < Profiles.MAX_NAME
                && !Profiles.cleanName("a" + ch).equals("a")) buffer.append(ch);
            return true;
        }
        switch (k) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> { select((index - 1 + rows()) % rows()); game.playSfx(Sfx.CLICK); }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> { select((index + 1) % rows()); game.playSfx(Sfx.CLICK); }
            case KeyEvent.VK_ENTER -> confirm();
            case KeyEvent.VK_ESCAPE -> { game.playSfx(Sfx.CONFIRM); game.state = State.MENU; }
            case KeyEvent.VK_R, KeyEvent.VK_F2 -> { if (selectedPilot() != null) startTyping(Typing.RENAME, selectedPilot().name()); }
            case KeyEvent.VK_DELETE -> removeSelected();
            default -> { return false; }
        }
        return true;
    }

    /** Delete, pressed twice: takes the pilot off the list. Their save is set aside, never deleted. */
    void removeSelected() {
        Profiles.Pilot p = selectedPilot();
        if (p == null) return;
        if (p.name().equalsIgnoreCase(SaveFile.pilotName == null ? "" : SaveFile.pilotName)
                || game.saveFile.file.getAbsoluteFile().equals(profiles.saveOf(p).getAbsoluteFile())) {
            message = "Switch to another pilot before removing " + p.name();
            game.playSfx(Sfx.CLICK);
            return;
        }
        if (!removeArmed) {
            removeArmed = true;
            message = "Press Delete again to remove " + p.name() + " (their save is kept as a .removed file)";
            game.playSfx(Sfx.CLICK);
            return;
        }
        try {
            profiles.remove(p);
            message = p.name() + " removed";
            index = clampInt(index, 0, rows() - 1);
            game.playSfx(Sfx.CONFIRM);
        } catch (IOException e) {
            message = "Could not remove " + p.name() + ": " + e.getMessage();
        }
        removeArmed = false;
    }

    String[] labels() {
        profiles();
        String[] out = new String[rows()];
        for (int i = 0; i < profiles.pilots.size(); i++) {
            String name = profiles.pilots.get(i).name();
            boolean playing = name.equalsIgnoreCase(SaveFile.pilotName == null ? "" : SaveFile.pilotName);
            out[i] = name + (playing ? "  (playing now)" : "");
        }
        out[newRow()] = typing == Typing.NEW ? "New pilot: " + buffer + "_" : "+ New Pilot";
        out[backRow()] = "Back";
        if (typing == Typing.RENAME && selectedPilot() != null) out[index] = "Rename to: " + buffer + "_";
        return out;
    }

    void draw(Graphics2D g2) {
        g2.setColor(new Color(6, 8, 18, 230));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        g2.setColor(Color.CYAN);
        g2.setFont(font(Font.BOLD, 38));
        game.drawCentered(g2, "WHO'S PLAYING?", GAME_WIDTH / 2, 70);
        g2.setColor(new Color(170, 185, 210));
        g2.setFont(font(Font.PLAIN, 16));
        game.drawCentered(g2, "Each pilot keeps their own progress, medals, notebook and settings.", GAME_WIDTH / 2, 100);

        index = clampInt(index, 0, rows() - 1);
        // Long lists squeeze together rather than run into the hints below.
        int gap = Math.min(ROW_GAP, (GAME_HEIGHT - 120 - LIST_TOP) / Math.max(1, rows() - 1));
        game.drawSelectableList(g2, labels(), index, LIST_TOP, gap, 22, 18);

        if (message != null) {
            g2.setFont(font(Font.BOLD, 14));
            g2.setColor(removeArmed ? new Color(255, 150, 110) : new Color(255, 215, 120));
            game.drawCentered(g2, message, GAME_WIDTH / 2, GAME_HEIGHT - 70);
        }
        g2.setColor(new Color(180, 180, 180));
        g2.setFont(font(Font.PLAIN, 14));
        game.drawCentered(g2, typing != Typing.NONE
            ? "Type a name  •  ENTER to save  •  ESC to cancel"
            : "ENTER or click to choose  •  R to rename  •  Delete to remove  •  ESC to go back", GAME_WIDTH / 2, GAME_HEIGHT - 28);
    }

    /** The corner label on the main menu: who is playing, and how to switch. */
    void drawMenuLabel(Graphics2D g2) {
        String text = SaveFile.pilotName != null ? "Pilot: " + SaveFile.pilotName + "   (P to switch)" : "Shared computer? P to add pilots";
        boolean hover = LABEL.contains(game.mouseGX, game.mouseGY);
        g2.setFont(font(Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text);
        g2.setColor(hover ? new Color(255, 230, 140) : new Color(150, 200, 235));
        g2.drawString(text, (int) (LABEL.getMaxX() - w), (int) LABEL.getY() + 15);
    }
}
