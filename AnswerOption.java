package rocketlearn;

import java.awt.Color;
import java.util.Random;

/**
 * One falling answer asteroid. Its silhouette, craters and rock colour are
 * generated randomly at spawn so no two look alike, tinted toward the planet
 * the player is currently visiting.
 */
class AnswerOption {

    String text;
    boolean correct;
    double x, y, speed, targetX;
    double rotation, rotationSpeed;
    double[] shapeR;
    double[][] craters;
    Color color;

    AnswerOption(String text, boolean correct, double x, double y, double speed, Random rng, Color planetTint) {
        this.text = text;
        this.correct = correct;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.targetX = x;
        this.rotation = rng.nextDouble() * Math.PI * 2;
        this.rotationSpeed = (rng.nextBoolean() ? 1 : -1) * (0.3 + rng.nextDouble() * 0.4);

        int points = 12;
        shapeR = new double[points];
        for (int i = 0; i < points; i++) shapeR[i] = 0.72 + rng.nextDouble() * 0.4;

        int craterCount = 3 + rng.nextInt(3);
        craters = new double[craterCount][3];
        for (int i = 0; i < craterCount; i++) {
            craters[i][0] = (rng.nextDouble() - 0.5) * 0.6;
            craters[i][1] = (rng.nextDouble() - 0.5) * 0.6;
            craters[i][2] = 0.1 + rng.nextDouble() * 0.12;
        }

        Color rock = new Color(120, 105, 90);
        double t = 0.4;
        int jitter = rng.nextInt(31) - 15;
        int rr = clampByte((int) (rock.getRed() * (1 - t) + planetTint.getRed() * t) + jitter);
        int gg = clampByte((int) (rock.getGreen() * (1 - t) + planetTint.getGreen() * t) + jitter);
        int bb = clampByte((int) (rock.getBlue() * (1 - t) + planetTint.getBlue() * t) + jitter);
        this.color = new Color(rr, gg, bb);
    }

    static int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
