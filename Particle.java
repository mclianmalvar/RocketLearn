package rocketlearn;

import java.awt.Color;

/** A short-lived spark, smoke puff or dust mote from an explosion or thruster. */
class Particle {

    double x, y, vx, vy, life, maxLife;
    Color color;
    double gravityScale;

    Particle(double x, double y, double vx, double vy, double life, Color color) {
        this(x, y, vx, vy, life, color, 1.0);
    }

    Particle(double x, double y, double vx, double vy, double life, Color color, double gravityScale) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
        this.gravityScale = gravityScale;
    }

    void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        vy += 300 * dt * gravityScale;
        life -= dt;
    }
}
