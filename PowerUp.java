package rocketlearn;

import java.awt.Color;
import java.util.Random;

/** A drifting collectible gem. */
class PowerUp {

    PowerType type;
    double x, y, speed, rotation, bob;

    PowerUp(PowerType type, double x, double y, double speed, Random rng) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.rotation = rng.nextDouble() * Math.PI * 2;
        this.bob = rng.nextDouble() * Math.PI * 2;
    }

    Color color() {
        switch (type) {
            case SHIELD: return new Color(90, 200, 255);
            case FREEZE: return new Color(160, 230, 255);
            default: return new Color(255, 200, 80);
        }
    }

    String label() {
        switch (type) {
            case SHIELD: return "SHIELD";
            case FREEZE: return "FREEZE";
            default: return "50/50";
        }
    }

    String describe() {
        switch (type) {
            case SHIELD: return "Absorbs one wrong answer";
            case FREEZE: return "Stops asteroids and the timer";
            default: return "Destroys two wrong answers";
        }
    }
}
