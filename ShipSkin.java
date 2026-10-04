package rocketlearn;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A paint job for the rocket. Classic is always available; each of the next
 * eight unlocks when the player finishes the matching planet, so the ship
 * becomes a record of how far they have travelled. Starborn is the star's
 * gift for finishing the journey, and after it come the eight Gilded paints,
 * each earned with a gold medal on its planet.
 */
class ShipSkin {

    /** Free from the start. */
    static final int UNLOCK_FREE = -1;
    /** Earned by clearing the star's gauntlet rather than by reaching a planet. */
    static final int UNLOCK_JOURNEY = -2;

    /** The artwork painted on the hull, over the paint and under the cockpit. */
    enum Decal { STRIPES, FLAMES, SUNBURST, WAVES, WEATHERED, CHECKER, FILIGREE, FROST, STARFIELD, CHEVRONS }

    /**
     * The look of the ship's shot. Only the look: every shot hits at the same
     * single point, so no paint is better or worse to fly than another.
     */
    enum LaserShape {
        BOLT("Bolt"), FIREBALL("Fireball"), ORB("Solar Orb"), HELIX("Tide Helix"), SLUG("Iron Slug"),
        TEARDROP("Honey Drop"), LANCE("Pearl Lance"), SHARD("Ice Shard"), VOID("Void Ring"), STAR("Starfire");

        final String label;
        LaserShape(String label) { this.label = label; }
    }

    final String name;
    final Color hullLight, hullDark, trim, finFill, finLine;
    /** Planet whose arrival unlocks this skin, or one of the UNLOCK_ constants. */
    final int unlockPlanet;
    /** Radiant ships carry a little of the star's light: a halo and a gold exhaust. */
    final boolean radiant;
    final Decal decal;
    final LaserShape laser;
    /** The shot's hot centre, and the colour of its glow, trail and muzzle flash. */
    final Color laserCore, laserGlow;
    /** True for the gold-trimmed version of a planet's paint, earned with a gold medal there. */
    final boolean gilded;

    ShipSkin(String name, Color hullLight, Color hullDark, Color trim, Color finFill, Color finLine,
             int unlockPlanet, boolean radiant, Decal decal, LaserShape laser, Color laserCore, Color laserGlow) {
        this(name, hullLight, hullDark, trim, finFill, finLine, unlockPlanet, radiant, decal, laser, laserCore, laserGlow, false);
    }

    ShipSkin(String name, Color hullLight, Color hullDark, Color trim, Color finFill, Color finLine,
             int unlockPlanet, boolean radiant, Decal decal, LaserShape laser, Color laserCore, Color laserGlow,
             boolean gilded) {
        this.gilded = gilded;
        this.name = name;
        this.hullLight = hullLight;
        this.hullDark = hullDark;
        this.trim = trim;
        this.finFill = finFill;
        this.finLine = finLine;
        this.unlockPlanet = unlockPlanet;
        this.radiant = radiant;
        this.decal = decal;
        this.laser = laser;
        this.laserCore = laserCore;
        this.laserGlow = laserGlow;
    }

    static final List<ShipSkin> ALL = withGilded(Arrays.asList(
        new ShipSkin("Classic",
            new Color(235, 238, 242), new Color(150, 158, 168), new Color(120, 126, 136),
            new Color(150, 60, 60), new Color(90, 30, 30), UNLOCK_FREE, false,
            Decal.STRIPES, LaserShape.BOLT, new Color(190, 250, 255), new Color(120, 240, 255)),
        new ShipSkin("Ember",
            new Color(245, 225, 210), new Color(170, 140, 125), new Color(140, 110, 96),
            new Color(190, 95, 50), new Color(120, 52, 24), 0, false,
            Decal.FLAMES, LaserShape.FIREBALL, new Color(255, 226, 150), new Color(255, 110, 40)),
        new ShipSkin("Sunspot",
            new Color(252, 243, 205), new Color(198, 168, 96), new Color(160, 134, 74),
            new Color(214, 160, 52), new Color(138, 98, 22), 1, false,
            Decal.SUNBURST, LaserShape.ORB, new Color(255, 250, 205), new Color(255, 196, 60)),
        new ShipSkin("Azure",
            new Color(226, 240, 252), new Color(120, 160, 205), new Color(92, 128, 170),
            new Color(60, 150, 105), new Color(28, 92, 62), 2, false,
            Decal.WAVES, LaserShape.HELIX, new Color(215, 248, 255), new Color(70, 170, 255)),
        new ShipSkin("Rust",
            new Color(246, 222, 210), new Color(180, 110, 88), new Color(146, 82, 62),
            new Color(168, 62, 42), new Color(104, 34, 22), 3, false,
            Decal.WEATHERED, LaserShape.SLUG, new Color(255, 205, 165), new Color(225, 92, 50)),
        new ShipSkin("Amber",
            new Color(250, 236, 212), new Color(196, 158, 112), new Color(158, 124, 84),
            new Color(190, 86, 62), new Color(122, 48, 32), 4, false,
            Decal.CHECKER, LaserShape.TEARDROP, new Color(255, 238, 175), new Color(240, 160, 50)),
        new ShipSkin("Ivory",
            new Color(252, 246, 228), new Color(202, 184, 142), new Color(164, 148, 110),
            new Color(186, 158, 104), new Color(120, 98, 58), 5, false,
            Decal.FILIGREE, LaserShape.LANCE, new Color(255, 253, 242), new Color(232, 210, 160)),
        new ShipSkin("Frost",
            new Color(232, 250, 252), new Color(140, 196, 208), new Color(106, 158, 172),
            new Color(96, 182, 192), new Color(46, 116, 128), 6, false,
            Decal.FROST, LaserShape.SHARD, new Color(238, 255, 255), new Color(140, 230, 255)),
        new ShipSkin("Void",
            new Color(214, 222, 250), new Color(96, 110, 190), new Color(72, 84, 150),
            new Color(60, 70, 175), new Color(28, 34, 104), 7, false,
            Decal.STARFIELD, LaserShape.VOID, new Color(36, 18, 66), new Color(175, 115, 255)),
        new ShipSkin("Starborn",
            new Color(255, 248, 222), new Color(222, 172, 64), new Color(178, 128, 34),
            new Color(255, 200, 70), new Color(150, 96, 18), UNLOCK_JOURNEY, true,
            Decal.CHEVRONS, LaserShape.STAR, new Color(255, 250, 222), new Color(255, 200, 70))
    ));

    /** Where Starborn sits in ALL. The Gilded paints come after it, so saved choices keep their numbers. */
    static final int STARBORN = 9;

    /** Gold leaf over a planet paint: the same art and laser, in gold trim with a gilded shot. */
    static ShipSkin gildedOf(ShipSkin base) {
        Color gold = new Color(222, 178, 58);
        return new ShipSkin("Gilded " + base.name,
            RocketLearn.lerpColor(base.hullLight, new Color(255, 246, 215), 0.35f),
            base.hullDark, gold,
            RocketLearn.lerpColor(base.finFill, gold, 0.35f), new Color(140, 98, 20),
            base.unlockPlanet, false, base.decal, base.laser,
            RocketLearn.lerpColor(base.laserCore, new Color(255, 248, 210), 0.35f),
            RocketLearn.lerpColor(base.laserGlow, gold, 0.45f), true);
    }

    /** The base paints followed by a Gilded version of each planet paint. */
    static List<ShipSkin> withGilded(List<ShipSkin> base) {
        List<ShipSkin> all = new ArrayList<>(base);
        for (int i = 1; i < STARBORN; i++) all.add(gildedOf(base.get(i)));
        return java.util.Collections.unmodifiableList(all);
    }

    /**
     * Skins the player has earned, Classic first. A planet's paint comes with
     * finishing that planet, its Gilded version with a gold medal there, and
     * Starborn with finishing the journey. Both arrays are indexed like Planet.ALL.
     */
    static List<ShipSkin> unlocked(boolean[] finished, boolean[] gold, boolean journeyComplete) {
        List<ShipSkin> out = new ArrayList<>();
        for (ShipSkin s : ALL) {
            boolean earned = s.unlockPlanet == UNLOCK_FREE ? true
                : s.unlockPlanet == UNLOCK_JOURNEY ? journeyComplete
                : s.gilded ? gold[s.unlockPlanet]
                : finished[s.unlockPlanet];
            if (earned) out.add(s);
        }
        return out;
    }

    String unlockLabel() {
        return unlockPlanet == UNLOCK_FREE ? "Available from the start"
            : unlockPlanet == UNLOCK_JOURNEY ? "The star's gift for finishing the journey"
            : gilded ? "Earned with a gold medal on " + Planet.ALL.get(unlockPlanet).name
            : "Earned by finishing " + Planet.ALL.get(unlockPlanet).name;
    }
}
