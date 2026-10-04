package rocketlearn;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The look of the main engine: its flame colour and what it sheds behind the
 * ship. Chosen separately from the paint. Standard is always available; each
 * of the others is earned by finishing its planet, and wears that world's
 * colours. Purely cosmetic - every trail flies the same.
 */
class EngineTrail {

    /** How the shed particles behave. */
    enum Kind {
        /** No extra particles: just the flame. */
        NONE,
        /** Slow, heavy puffs that hang and sink. */
        DUST,
        /** Soft, slow mist that drifts and spreads. */
        MIST,
        /** Quick bright sparks that fly out and fall. */
        SPARKS,
        /** Shimmering motes that rise a little as they fade. */
        SHIMMER
    }

    final String name;
    /** Index into Planet.ALL whose finishing earns it, or -1 for always available. */
    final int planet;
    final Color flame;
    final Kind kind;
    /** Particle colours, picked from at random. */
    final Color[] particles;

    EngineTrail(String name, int planet, Color flame, Kind kind, Color... particles) {
        this.name = name;
        this.planet = planet;
        this.flame = flame;
        this.kind = kind;
        this.particles = particles;
    }

    static final List<EngineTrail> ALL = Arrays.asList(
        new EngineTrail("Standard", -1, Color.ORANGE, Kind.NONE),
        new EngineTrail("Aurora", 2, new Color(90, 235, 170), Kind.SHIMMER,
            new Color(110, 255, 180), new Color(120, 200, 255), new Color(200, 140, 255)),
        new EngineTrail("Red Dust", 3, new Color(235, 105, 60), Kind.DUST,
            new Color(190, 90, 60), new Color(160, 70, 45), new Color(215, 130, 90)),
        new EngineTrail("Storm Bands", 4, new Color(240, 170, 100), Kind.MIST,
            new Color(230, 180, 130), new Color(200, 120, 80), new Color(245, 225, 190)),
        new EngineTrail("Ring Dust", 5, new Color(245, 215, 140), Kind.SHIMMER,
            new Color(255, 235, 170), new Color(230, 205, 150), new Color(255, 255, 230)),
        new EngineTrail("Cyan Mist", 6, new Color(120, 225, 235), Kind.MIST,
            new Color(150, 230, 240), new Color(110, 200, 215), new Color(215, 250, 255)),
        new EngineTrail("Deep Gale", 7, new Color(80, 120, 255), Kind.SPARKS,
            new Color(90, 130, 255), new Color(150, 170, 255), new Color(60, 80, 210)),
        new EngineTrail("Acid Haze", 1, new Color(225, 215, 90), Kind.MIST,
            new Color(225, 215, 110), new Color(190, 200, 80), new Color(245, 235, 160)),
        new EngineTrail("Solar Wind", 0, new Color(255, 245, 210), Kind.SPARKS,
            new Color(255, 255, 240), new Color(255, 220, 140), new Color(255, 180, 90))
    );

    /** Trails the player has earned, Standard first. */
    static List<EngineTrail> unlocked(boolean[] finished) {
        List<EngineTrail> out = new ArrayList<>();
        for (EngineTrail t : ALL) if (t.planet < 0 || finished[t.planet]) out.add(t);
        return out;
    }

    String unlockLabel() {
        return planet < 0 ? "Available from the start" : "Earned by finishing " + Planet.ALL.get(planet).name;
    }
}
