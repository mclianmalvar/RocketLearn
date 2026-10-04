package rocketlearn;

/** Which screen the game is currently showing. */
enum State {
    MENU,
    /** The settings hub: one row per group of settings, plus Controls. */
    SETTINGS_MENU,
    /** One group's settings page, chosen from the hub. */
    SETTINGS,
    CONTROLS, LEVEL_SELECT, HOW_TO_PLAY,
    PLAYING, PAUSED, GAME_OVER, VICTORY, REVIEW, NAME_ENTRY, HIGH_SCORES,
    /** The star speaks once the player reaches it, and offers them the choice. */
    STAR,
    /** The practice hub: drills, the notebook, and per-topic stats. */
    STUDY,
    /** Every question the player has missed, with its explanation, to reread. */
    NOTEBOOK,
    /** Who's playing: pick, add, rename or remove a pilot. */
    PILOTS,
    /** Lifetime accuracy for every topic. */
    TOPIC_STATS,
    /** A card for every finished planet: its facts, topics and medal. */
    CODEX
}
