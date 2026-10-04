package rocketlearn;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static rocketlearn.RocketLearn.*;

/**
 * What finishing planets earns, and keeping the player aware of it: which
 * paints and trails are unlocked, the finished-planet banner with its medal
 * and unlocks, and the NEW marks on rewards not yet looked at.
 *
 * The medals themselves are kept by PlanetRecords; this decides what they
 * open up and how the player hears about it.
 */
class Rewards {

    final RocketLearn game;

    Rewards(RocketLearn game) { this.game = game; }

    // ---- what is unlocked

    List<ShipSkin> unlockedSkins() {
        if (UNLOCK_ALL_SKINS) return ShipSkin.ALL;
        return ShipSkin.unlocked(finishedPlanets(), goldPlanets(), game.journeyComplete);
    }

    List<EngineTrail> unlockedTrails() {
        if (UNLOCK_ALL_SKINS) return EngineTrail.ALL;
        return EngineTrail.unlocked(finishedPlanets());
    }

    /** Which planets are finished, indexed like Planet.ALL. */
    boolean[] finishedPlanets() {
        boolean[] out = new boolean[planets.size()];
        for (int p = 0; p < out.length; p++) out[p] = game.planetRecords.finished(p, game.highestLevelReached);
        return out;
    }

    /** Which planets have a gold medal, indexed like Planet.ALL. */
    boolean[] goldPlanets() {
        boolean[] out = new boolean[planets.size()];
        for (int p = 0; p < out.length; p++) out[p] = game.planetRecords.gold(p);
        return out;
    }

    /** A paint or trail that is no longer earned (an old save, a testing flag turned off) falls back to the first. */
    void keepCosmeticsEarned() {
        if (!unlockedSkins().contains(game.currentSkin())) game.shipSkinIndex = 0;
        if (!unlockedTrails().contains(game.currentTrail())) game.trailIndex = 0;
    }

    // ---- finishing a planet

    /** The finished-planet banner waiting to be shown, and what follows it. */
    String finishedTitle, finishedSubtitle;
    Color finishedColor = Color.WHITE;
    BannerAction afterPlanetFinished = BannerAction.NEXT_QUESTION;

    /**
     * Records a planet as finished and prepares its banner: the medal this run
     * earned and how, and whatever it unlocked for the first time - which is
     * also marked NEW where the player will find it.
     */
    void finishPlanet(int planet, int highestBefore) {
        PlanetRecords records = game.planetRecords;
        boolean wasFinished = records.finished(planet, highestBefore);
        boolean wasGold = records.gold(planet);
        int lost = records.heartsLost;
        int pct = (int) Math.round(records.accuracy() * 100);
        boolean wholePlanet = records.fromStart && records.tracking == planet;
        PlanetRecords.Medal earned = records.finishPlanet(planet);
        PlanetRecords.Medal best = records.best[planet];

        finishedTitle = planets.get(planet).name.toUpperCase() + " FINISHED!";
        finishedColor = earned.color;
        String how = wholePlanet
            ? (lost == 0 ? "no hearts lost" : lost + (lost == 1 ? " heart" : " hearts") + " lost") + ", " + pct + "% right first try"
            : "joined part way - fly it from its first level for silver or gold";
        finishedSubtitle = earned.label.toUpperCase() + " MEDAL  -  " + how
            + (best != earned ? "\nYour best here is still " + best.label : "");

        List<String> unlocked = new ArrayList<>();
        if (!wasFinished) {
            for (ShipSkin s : ShipSkin.ALL) if (s.unlockPlanet == planet && !s.gilded) unlocked.add(s.name + " paint");
            for (EngineTrail t : EngineTrail.ALL) if (t.planet == planet) unlocked.add(t.name + " trail");
            unlocked.add("codex card");
            unlocked.add("hull badge");
            unseen.add(UNSEEN_SKIN);
            unseen.add(UNSEEN_TRAIL);
            unseen.add(UNSEEN_CODEX + planet);
        }
        if (!wasGold && records.gold(planet)) {
            for (ShipSkin s : ShipSkin.ALL) if (s.unlockPlanet == planet && s.gilded) unlocked.add(s.name + " paint");
            unseen.add(UNSEEN_SKIN);
        }
        if (!unlocked.isEmpty()) finishedSubtitle += "\nNew: " + String.join(", ", unlocked);
        else if (best != PlanetRecords.Medal.GOLD) finishedSubtitle += "\n" + PlanetRecords.nextGoal(best);
    }

    /** The finished-planet banner: held until the player presses on, with a fanfare and a burst of medal-coloured sparks. */
    void showPlanetFinishedBanner() {
        game.showBanner(finishedTitle, finishedColor, 3.0, afterPlanetFinished, true, finishedSubtitle);
        game.bannerAwaitInput = true;
        game.bannerHidePanel = true;
        game.playSfx(Sfx.PLANET_FANFARE);
        for (int i = 0; i < 40; i++) {
            double ang = game.rng.nextDouble() * Math.PI * 2, spd = 80 + game.rng.nextDouble() * 200;
            Color c = game.rng.nextInt(3) == 0 ? Color.WHITE : finishedColor;
            game.particles.add(new Particle(GAME_WIDTH / 2.0, GAME_HEIGHT / 2.0, Math.cos(ang) * spd, Math.sin(ang) * spd - 60,
                0.6 + game.rng.nextDouble() * 0.6, c));
        }
    }

    // ---- rewards not yet looked at
    //
    // Each finished planet marks its new paint, trail and codex card NEW
    // until the player has been to see them, so a reward is never missed.

    static final String UNSEEN_SKIN = "skin", UNSEEN_TRAIL = "trail", UNSEEN_CODEX = "codex:";
    final Set<String> unseen = new LinkedHashSet<>();

    boolean skinsUnseen() { return game.starbornUnseen || unseen.contains(UNSEEN_SKIN); }
    boolean trailsUnseen() { return unseen.contains(UNSEEN_TRAIL); }
    boolean codexUnseen(int planet) { return unseen.contains(UNSEEN_CODEX + planet); }
    boolean anyCodexUnseen() {
        for (String u : unseen) if (u.startsWith(UNSEEN_CODEX)) return true;
        return false;
    }

    /** Where the codex opens: on the furthest card not yet read, or the first card. */
    int codexOpeningSlot() {
        for (int slot = TRAVEL_ORDER.length - 1; slot >= 0; slot--) if (codexUnseen(TRAVEL_ORDER[slot])) return slot;
        return 0;
    }

    /** Marks as seen whatever the current screen is showing. Runs every tick. */
    void noteSeen() {
        if (game.state == State.SETTINGS && game.settings.currentRow() == SettingsScreen.Row.SHIP) {
            game.starbornUnseen = false;
            unseen.remove(UNSEEN_SKIN);
        }
        if (game.state == State.SETTINGS && game.settings.currentRow() == SettingsScreen.Row.TRAIL) unseen.remove(UNSEEN_TRAIL);
        if (game.state == State.CODEX) unseen.remove(UNSEEN_CODEX + TRAVEL_ORDER[Math.floorMod(game.codexIndex, TRAVEL_ORDER.length)]);
    }
}
