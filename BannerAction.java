package rocketlearn;

/** What should happen once the current banner finishes showing. */
enum BannerAction {
    NONE, NEXT_QUESTION, GAME_OVER, VICTORY, SHOW_ARRIVAL,
    /** A planet was just finished: its medal and unlocks, held until the player presses on. */
    PLANET_FINISHED,
    /** A medal challenge's planet is finished: the run ends and Level Select returns. */
    CHALLENGE_DONE,
    /** The star introduces its final test. */
    STAR_INTRO,
    /** The star's closing words, once its level is cleared. */
    STAR_REACHED
}
