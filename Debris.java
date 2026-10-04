package rocketlearn;

import java.util.Random;

/** A loose rock from Saturn's rings. It blocks shots but never harms the rocket. */
class Debris {

    double x, y, speed, radius, rotation, rotSpeed;

    Debris(double x, double y, double speed, double radius, Random rng) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.radius = radius;
        this.rotation = rng.nextDouble() * Math.PI * 2;
        this.rotSpeed = (rng.nextBoolean() ? 1 : -1) * (0.6 + rng.nextDouble() * 0.8);
    }
}
