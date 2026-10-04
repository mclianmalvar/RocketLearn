package rocketlearn;

/**
 * The look of the space around each destination. Every planet gets its own
 * atmosphere so the nine stops read as different places rather than the same
 * starfield in a different tint.
 */
enum Environment {
    /** Mercury: hammered by sunlight, with heat shimmering off the hull. */
    SUN_BLAST,
    /** Venus: thick sulphuric cloud decks rolling past. */
    CLOUD_BANDS,
    /** Earth: calm, with auroras curtaining the upper atmosphere. */
    AURORA,
    /** Mars: rust-coloured dust driven across the field in gusts. */
    DUST_STORM,
    /** Jupiter: banded cloud tops lit from within by enormous storms. */
    THUNDER,
    /** Saturn: flying through the ring plane, ice glinting as it passes. */
    RING_PLANE,
    /** Uranus: a slow drift of methane ice crystals. */
    ICE_FIELD,
    /** Neptune: supersonic wind tearing past in visible streaks. */
    WIND_STREAKS,
    /** The star: embers rising through the glare of the corona. */
    CORONA
}
