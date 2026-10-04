package rocketlearn;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * A destination planet. Beyond its colours, each planet carries a trait that
 * changes how the level actually plays - a shorter timer, heavier gravity,
 * reduced friction and so on - plus a fact shown when the player arrives.
 *
 * Traits are attached with the chained helpers below, so adding or tuning one
 * is a single call in the ALL list.
 */
class Planet {

    final String name;
    final Color base, accent;
    final boolean ringed;
    final String fact, traitName, traitDesc;

    double timeScale = 1.0;
    double fallScale = 1.0;
    double frictionValue = 0.85;
    double windAccel = 0;
    boolean haze = false, dust = false;
    int debrisCount = 0;
    Environment env = Environment.AURORA;

    Planet(String name, Color base, Color accent, boolean ringed, String fact, String traitName, String traitDesc) {
        this.name = name;
        this.base = base;
        this.accent = accent;
        this.ringed = ringed;
        this.fact = fact;
        this.traitName = traitName;
        this.traitDesc = traitDesc;
    }

    Planet time(double v) { timeScale = v; return this; }
    Planet fall(double v) { fallScale = v; return this; }
    Planet friction(double v) { frictionValue = v; return this; }
    Planet wind(double v) { windAccel = v; return this; }
    Planet haze() { haze = true; return this; }
    Planet dust() { dust = true; return this; }
    Planet debris(int n) { debrisCount = n; return this; }
    Planet env(Environment e) { env = e; return this; }

    /** True when this planet applies no gameplay modifier at all. */
    boolean isCalm() {
        return timeScale == 1.0 && fallScale == 1.0 && frictionValue == 0.85
            && windAccel == 0 && !haze && !dust && debrisCount == 0;
    }

    /** Uranus orbits on its side, so its rings stand almost upright. */
    static double ringTilt(int planetIdx) {
        return planetIdx == 6 ? 1.30 : -0.35;
    }

    /**
     * The true final destination, reached after Neptune. It is deliberately not
     * part of ALL - the eight planets drive level select, the orbit track and
     * the difficulty sectors, while the star sits beyond all of them.
     */
    static final Planet STAR = new Planet("The Star",
        new Color(255, 242, 190), new Color(255, 178, 70), false,
        "Every world you passed is lit by this. A furnace turning hydrogen into light.",
        "THE SUMMARY", "Every question from every world, and flares between them")
        // Gravity is capped just under Jupiter's on purpose: BalanceCheck shows
        // anything heavier makes the boss unfair even when the player knows the
        // answer. Its difficulty comes from the timer, the shuffling and the flares.
        .time(0.72).fall(1.33).haze().env(Environment.CORONA);

    static final List<Planet> ALL = Arrays.asList(
        new Planet("Mercury", new Color(170, 160, 150), new Color(120, 110, 100), false,
            "Closest planet to the Sun, and the smallest in the solar system.",
            "SCORCHING", "Blistering heat - the timer runs 25% shorter").time(0.75).env(Environment.SUN_BLAST),
        new Planet("Venus", new Color(230, 200, 140), new Color(200, 160, 90), false,
            "The hottest planet, wrapped in thick clouds of sulphuric acid.",
            "BLINDING HAZE", "Dense cloud cover hides the field").haze().env(Environment.CLOUD_BANDS),
        new Planet("Earth", new Color(70, 130, 220), new Color(60, 180, 90), false,
            "The only world known to support life, and 71% of it is ocean.",
            "HOME TURF", "Clear skies - standard conditions").env(Environment.AURORA),
        new Planet("Mars", new Color(205, 95, 65), new Color(150, 60, 40), false,
            "The red planet, home to Olympus Mons - the tallest volcano known.",
            "DUST STORMS", "Blowing dust sweeps across the field").dust().env(Environment.DUST_STORM),
        new Planet("Jupiter", new Color(215, 175, 130), new Color(170, 120, 80), false,
            "The largest planet - over 1,300 Earths would fit inside it.",
            "CRUSHING GRAVITY", "Asteroids fall 35% faster").fall(1.35).env(Environment.THUNDER),
        new Planet("Saturn", new Color(230, 205, 155), new Color(190, 160, 110), true,
            "Famous for its rings, made mostly of ice and rock.",
            "RING DEBRIS", "Loose rocks drift through and block your shots").debris(2).env(Environment.RING_PLANE),
        new Planet("Uranus", new Color(150, 215, 220), new Color(100, 180, 190), true,
            "Tipped on its side, it orbits the Sun rolling like a barrel.",
            "ICE SLICK", "Almost no friction - the rocket slides").friction(0.975).env(Environment.ICE_FIELD),
        new Planet("Neptune", new Color(75, 95, 220), new Color(50, 60, 180), false,
            "The windiest world, with gusts topping 2,000 km/h.",
            "SUPERSONIC WINDS", "Constant sideways gusts push you off course").wind(145).env(Environment.WIND_STREAKS)
    );
}
