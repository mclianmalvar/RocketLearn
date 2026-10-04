package rocketlearn;

import java.awt.geom.Path2D;

/**
 * The silhouette of an asteroid held on screen for a few frames after it is
 * hit, drawn in white so the moment of contact reads clearly before the rock
 * breaks apart into particles.
 */
class HitFlash {

    final Path2D shape;
    double life;
    final double maxLife;

    HitFlash(Path2D shape, double life) {
        this.shape = shape;
        this.life = life;
        this.maxLife = life;
    }
}
