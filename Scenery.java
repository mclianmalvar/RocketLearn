package rocketlearn;

import java.awt.*;
import java.awt.geom.*;

import static rocketlearn.RocketLearn.*;

/**
 * Paints the world behind the play: each planet's backdrop, atmosphere and
 * weather, the star and its face, and the orbit track along the top.
 */
class Scenery {

    final RocketLearn game;

    Scenery(RocketLearn game) { this.game = game; }

    /** The vignette and the fog, painted once and stamped every frame after. */
    final OverlayCache overlays = new OverlayCache();

    /**
     * The planet's faint tint now travels inside the vignette's stamp (see
     * drawSceneryVignette), so it costs nothing of its own. Kept so the
     * drawing order still reads in one place.
     */
    void drawPlanetAtmosphere(Graphics2D g2) { }

    /** The planet's own faint colour wash, at most 7% strong: High only. */
    void paintTint(Graphics2D ig, Planet p) {
        ig.setPaint(new GradientPaint(
            0, 0, new Color(p.base.getRed(), p.base.getGreen(), p.base.getBlue(), 18),
            0, GAME_HEIGHT, new Color(p.accent.getRed(), p.accent.getGreen(), p.accent.getBlue(), 0)));
        ig.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
    }

    /**
     * Pushes the scenery back into shadow so the asteroids, rocket and shots
     * read cleanly on top of it. Without this the background washes compete
     * with the things the player actually has to see.
     *
     * On High the planet's tint is painted into the same stamp. Each
     * screen-wide layer costs a blend of every pixel - measured at about
     * 7 ms a frame in fullscreen without a graphics card - and layers laid
     * one over another can be merged first with the same result, so two
     * layers become one. The tint now sits over the weather instead of
     * under it, which at 7% strength does not show.
     */
    void drawSceneryVignette(Graphics2D g2) {
        Planet p = game.currentPlanet();
        boolean tinted = game.graphics.liveWashes;
        overlays.draw(g2, tinted ? "vignette:" + p.name : "vignette", 0, 0, GAME_WIDTH, GAME_HEIGHT, ig -> {
            if (tinted) paintTint(ig, p);
            ig.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0, GAME_HEIGHT * 0.46), (float) (GAME_WIDTH * 0.66),
                new float[]{0f, 0.46f, 1f},
                new Color[]{
                    new Color(0, 0, 0, 96),
                    new Color(0, 0, 0, 130),
                    new Color(0, 0, 0, 205)
                }));
            ig.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        });
    }

    /**
     * Scenery behind the falling asteroids: cloud decks, aurora curtains, storm
     * bands and the like. Deterministic from envPhase, so nothing is stored.
     */
    void drawEnvironmentBack(Graphics2D g2) {
        Planet p = game.currentPlanet();
        Paint old = g2.getPaint();

        switch (p.env) {
            case SUN_BLAST -> {
                // Tight and hot in the corner, falling away fast so the starfield survives.
                double pulse = 0.5 + 0.5 * Math.sin(game.envPhase * 0.8);
                g2.setPaint(new RadialGradientPaint(
                    new Point2D.Double(GAME_WIDTH * 0.95, -60), (float) (GAME_WIDTH * 0.62),
                    new float[]{0f, 0.38f, 1f},
                    new Color[]{
                        new Color(255, 238, 200, (int) (86 + pulse * 26)),
                        new Color(255, 196, 120, 30),
                        new Color(255, 165, 75, 0)
                    }));
                g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            }
            case CLOUD_BANDS -> {
                for (int i = 0; i < 7; i++) {
                    double y = (i * 86 + game.envPhase * (9 + i * 3)) % (GAME_HEIGHT + 130) - 65;
                    double h = 34 + (i % 3) * 16;
                    g2.setPaint(new GradientPaint(
                        0, (float) y, new Color(176, 130, 44, 0),
                        0, (float) (y + h / 2), new Color(182, 136, 48, 46)));
                    g2.fill(new Ellipse2D.Double(-120 + Math.sin(game.envPhase * 0.2 + i) * 50, y, GAME_WIDTH + 240, h));
                }
            }
            case AURORA -> {
                for (int i = 0; i < 3; i++) {
                    Path2D curtain = new Path2D.Double();
                    double baseY = 38 + i * 26;
                    curtain.moveTo(-40, baseY);
                    for (int x = -40; x <= GAME_WIDTH + 40; x += 30) {
                        double wave = Math.sin(x * 0.006 + game.envPhase * (0.5 + i * 0.16) + i) * 20;
                        curtain.lineTo(x, baseY + wave);
                    }
                    for (int x = GAME_WIDTH + 40; x >= -40; x -= 30) {
                        double wave = Math.sin(x * 0.006 + game.envPhase * (0.5 + i * 0.16) + i) * 20;
                        curtain.lineTo(x, baseY + wave + 62 + i * 12);
                    }
                    curtain.closePath();
                    Color top = i == 1 ? new Color(120, 210, 255) : new Color(110, 235, 170);
                    g2.setPaint(new GradientPaint(
                        0, (float) baseY, new Color(top.getRed(), top.getGreen(), top.getBlue(), 62),
                        0, (float) (baseY + 80), new Color(top.getRed(), top.getGreen(), top.getBlue(), 0)));
                    g2.fill(curtain);
                }
            }
            case DUST_STORM -> {
                double gust = 0.5 + 0.5 * Math.sin(game.envPhase * 0.35);
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(148, 44, 18, (int) (48 + gust * 46)),
                    0, GAME_HEIGHT, new Color(88, 20, 8, (int) (92 + gust * 60))));
                g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            }
            case THUNDER -> {
                for (int i = 0; i < 6; i++) {
                    double y = (i * 96 + game.envPhase * 11) % (GAME_HEIGHT + 120) - 60;
                    g2.setColor(i % 2 == 0 ? new Color(122, 76, 34, 58) : new Color(62, 36, 16, 54));
                    g2.fill(new Ellipse2D.Double(-80, y, GAME_WIDTH + 160, 46));
                }
            }
            case RING_PLANE -> {
                // A band of ice sliding across the screen at a shallow angle.
                // Fade in AND out across the band. A plain GradientPaint holds its
                // end colour past the last stop, which left a hard line on the edge.
                float bandTop = (float) (GAME_HEIGHT * 0.12), bandBottom = (float) (GAME_HEIGHT * 0.50);
                g2.setPaint(new LinearGradientPaint(
                    new Point2D.Float(0, bandTop), new Point2D.Float(0, bandBottom),
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{
                        new Color(226, 212, 180, 0),
                        new Color(226, 212, 180, 58),
                        new Color(226, 212, 180, 0)
                    }));
                g2.fillRect(0, (int) bandTop, GAME_WIDTH, (int) (bandBottom - bandTop));
                g2.setPaint(old);
                for (int i = 0, n = game.graphics.count(150); i < n; i++) {
                    double seed = i * 97.13;
                    double speed = 55 + (i % 7) * 26;
                    double x = ((seed + game.envPhase * speed) % (GAME_WIDTH + 200)) - 100;
                    double y = GAME_HEIGHT * 0.26 + ((seed * 1.7) % 150) - 24 + x * 0.10;
                    double s = 1.4 + (i % 4) * 1.1;
                    int a = 95 + (i % 5) * 32;
                    g2.setColor(new Color(240, 232, 208, a));
                    g2.fill(new Ellipse2D.Double(x, y, s, s));
                }
            }
            case ICE_FIELD -> {
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(18, 88, 108, 66),
                    0, GAME_HEIGHT, new Color(8, 50, 72, 110)));
                g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            }
            case WIND_STREAKS -> {
                double dir = Math.sin(game.windPhase * 0.6);
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(22, 30, 118, 88),
                    0, GAME_HEIGHT, new Color(8, 12, 68, 132)));
                g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                for (int i = 0, n = game.graphics.count(34); i < n; i++) {
                    double seed = i * 131.7;
                    double speed = (180 + (i % 6) * 120) * (dir >= 0 ? 1 : -1);
                    double x = (((seed + game.envPhase * speed) % (GAME_WIDTH + 400)) + GAME_WIDTH + 400) % (GAME_WIDTH + 400) - 200;
                    double y = (seed * 3.1) % GAME_HEIGHT;
                    double len = (26 + (i % 5) * 34) * Math.abs(dir);
                    g2.setColor(new Color(190, 215, 255, 40 + (i % 4) * 26));
                    g2.setStroke(new BasicStroke(1f + (i % 3) * 0.6f));
                    g2.draw(new Line2D.Double(x, y, x - len * Math.signum(speed), y));
                }
                g2.setStroke(new BasicStroke(1f));
            }
            case CORONA -> {
                double pulse = 0.5 + 0.5 * Math.sin(game.envPhase * 1.1);
                double charge = game.starFlareCharge > 0 ? 1 - clamp(game.starFlareCharge / FLARE_CHARGE_TIME, 0, 1) : 0;
                g2.setPaint(new GradientPaint(
                    0, GAME_HEIGHT, new Color(146, 36, 6, (int) (96 + pulse * 30 + charge * 60)),
                    0, 0, new Color(44, 8, 4, 130)));
                g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            }
            default -> { }
        }
        g2.setPaint(old);
    }

    /** Scenery in front of the action: shimmer, ice motes, lightning, glare. */
    void drawEnvironmentFront(Graphics2D g2) {
        Planet p = game.currentPlanet();
        Paint old = g2.getPaint();

        switch (p.env) {
            case SUN_BLAST -> {
                // Heat shimmer: thin bright bands rippling upward.
                for (int i = 0; i < 6; i++) {
                    double y = (i * 88 - game.envPhase * 26) % (GAME_HEIGHT + 60);
                    if (y < 0) y += GAME_HEIGHT + 60;
                    double wob = Math.sin(game.envPhase * 2.2 + i) * 5;
                    g2.setColor(new Color(255, 232, 186, 16));
                    g2.fill(new Ellipse2D.Double(-40 + wob, y - 30, GAME_WIDTH + 80, 6));
                }
            }
            case ICE_FIELD -> {
                // Drifting methane crystals, turning as they fall.
                for (int i = 0, n = game.graphics.count(60); i < n; i++) {
                    double seed = i * 73.31;
                    double speed = 16 + (i % 6) * 11;
                    double y = ((seed + game.envPhase * speed) % (GAME_HEIGHT + 60)) - 30;
                    double x = (seed * 2.3) % GAME_WIDTH + Math.sin(game.envPhase * 0.6 + i) * 14;
                    double s = 2.6 + (i % 4) * 1.9;
                    double spin = game.envPhase * (0.7 + (i % 3) * 0.4) + i;
                    AffineTransform tx = g2.getTransform();
                    g2.translate(x, y);
                    g2.rotate(spin);
                    g2.setColor(new Color(225, 248, 255, 110 + (i % 4) * 32));
                    Path2D crystal = new Path2D.Double();
                    crystal.moveTo(0, -s);
                    crystal.lineTo(s * 0.55, 0);
                    crystal.lineTo(0, s);
                    crystal.lineTo(-s * 0.55, 0);
                    crystal.closePath();
                    g2.fill(crystal);
                    g2.setTransform(tx);
                }
            }
            case RING_PLANE -> {
                // Occasional glints off passing ice.
                for (int i = 0; i < 5; i++) {
                    double t = (game.envPhase * 0.7 + i * 0.37) % 1.0;
                    if (t > 0.18) continue;
                    double a = Math.sin(t / 0.18 * Math.PI);
                    double x = ((i * 271.3) % GAME_WIDTH);
                    double y = GAME_HEIGHT * 0.30 + ((i * 173.1) % 120) - 20 + x * 0.10;
                    g2.setColor(new Color(255, 255, 255, (int) (a * 210)));
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.draw(new Line2D.Double(x - 7, y, x + 7, y));
                    g2.draw(new Line2D.Double(x, y - 7, x, y + 7));
                }
                g2.setStroke(new BasicStroke(1f));
            }
            case CORONA -> {
                double charge = game.starFlareCharge > 0 ? 1 - clamp(game.starFlareCharge / FLARE_CHARGE_TIME, 0, 1) : 0;
                double burn = game.starFlareActive > 0 ? clamp(game.starFlareActive / FLARE_ACTIVE_TIME, 0, 1) : 0;

                // Warning: a ring of light draws inward from the edges.
                if (charge > 0) {
                    float ringR = (float) (GAME_WIDTH * (1.15 - charge * 0.55));
                    g2.setPaint(new RadialGradientPaint(
                        new Point2D.Double(GAME_WIDTH, GAME_HEIGHT * 0.52), ringR,
                        new float[]{0f, 0.82f, 0.94f, 1f},
                        new Color[]{
                            new Color(255, 200, 90, 0),
                            new Color(255, 200, 90, 0),
                            new Color(255, 232, 160, (int) (110 * charge)),
                            new Color(255, 200, 90, 0)
                        }));
                    g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                }

                // The flare itself: a hot wash that burns off over a second.
                if (burn > 0) {
                    g2.setColor(new Color(255, 236, 180, (int) (burn * burn * 130)));
                    g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                    double waveR = GAME_WIDTH * (1 - burn) * 1.3;
                    g2.setStroke(new BasicStroke((float) (10 * burn)));
                    g2.setColor(new Color(255, 245, 205, (int) (burn * 170)));
                    g2.draw(new Ellipse2D.Double(GAME_WIDTH - waveR, GAME_HEIGHT * 0.52 - waveR, waveR * 2, waveR * 2));
                    g2.setStroke(new BasicStroke(1f));
                }
            }
            case THUNDER -> {
                if (game.lightningFlash > 0) {
                    float a = (float) clamp(game.lightningFlash / 0.32, 0, 1);
                    g2.setColor(new Color(255, 250, 225, (int) (a * a * 62 * game.flashScale())));
                    g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                    if (game.lightningFlash > 0.18) {
                        double x = (game.boltSeed % (GAME_WIDTH - 200)) + 100;
                        Path2D bolt = new Path2D.Double();
                        bolt.moveTo(x, -10);
                        double bx = x, by = -10;
                        for (int i = 0; i < 7; i++) {
                            bx += ((game.boltSeed * (i + 3)) % 60) - 30;
                            by += 26 + ((game.boltSeed * (i + 5)) % 22);
                            bolt.lineTo(bx, by);
                        }
                        g2.setStroke(new BasicStroke(2.4f));
                        g2.setColor(new Color(255, 250, 230, 215));
                        g2.draw(bolt);
                        g2.setStroke(new BasicStroke(6f));
                        g2.setColor(new Color(255, 236, 170, 70));
                        g2.draw(bolt);
                        g2.setStroke(new BasicStroke(1f));
                    }
                }
            }
            default -> { }
        }
        g2.setPaint(old);
        drawPlanetHaze(g2);
    }

    void drawPlanetHaze(Graphics2D g2) {
        Planet p = game.currentPlanet();
        if (!p.haze) return;
        // Fog that hides the field by going DARK, not pale. A bright wash washed
        // the whole screen out and left nothing readable.
        double drift = Math.sin(game.flameFlicker * 0.12) * 60;
        Color murk = lerpColor(p.accent, Color.BLACK, 0.68f);
        // The fog only slides from side to side, so it is painted once, wide
        // enough to cover the screen at either end of its drift, and moved.
        int reach = 60;
        // Kept to the game's own frame, or its spare width would show in the
        // black bars beside the game on a wide screen.
        Shape oldClip = g2.getClip();
        g2.clipRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        overlays.draw(g2, "haze:" + murk.getRGB(), drift - reach, 0, GAME_WIDTH + reach * 2, GAME_HEIGHT, ig -> {
            ig.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0 + reach, GAME_HEIGHT * 0.45), (float) (GAME_WIDTH * 0.78),
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                    new Color(murk.getRed(), murk.getGreen(), murk.getBlue(), 0),
                    new Color(murk.getRed(), murk.getGreen(), murk.getBlue(), 90),
                    new Color(murk.getRed(), murk.getGreen(), murk.getBlue(), 205)
                }));
            ig.fillRect(0, 0, GAME_WIDTH + reach * 2, GAME_HEIGHT);
        });
        g2.setClip(oldClip);
    }

    /**
     * The star's face. Its eyes follow the rocket, it smiles while it is simply
     * watching, opens its mouth while speaking, and scowls when it flares.
     *
     * @param lookX where the star should be looking, or -1 to gaze straight out
     */
    void drawStarFace(Graphics2D g2, double cx, double cy, double r, int mood, double lookX) {
        boolean fierce = mood == FACE_FIERCE || mood == FACE_RAGE;
        boolean smug = mood == FACE_SMUG;
        double eyeDX = r * 0.37, eyeY = cy - r * 0.10;
        double eyeW = r * 0.30, eyeH = r * (fierce ? 0.20 : 0.24);

        // Near-black features so they punch against the burning surface.
        Color ink = new Color(46, 10, 4, 245);
        Color glow = mood == FACE_RAGE ? new Color(255, 240, 220)
            : fierce ? new Color(255, 80, 40)
            : smug ? new Color(255, 190, 70)
            : new Color(255, 150, 40);

        double gaze = lookX < 0 ? 0 : clamp((lookX - cx) / (GAME_WIDTH * 0.7), -1, 1);
        double pupilShift = gaze * eyeW * 0.26;

        for (int side = -1; side <= 1; side += 2) {
            double ex = cx + side * eyeDX;
            double innerX = ex - side * eyeW * 0.55;
            double outerX = ex + side * eyeW * 0.55;

            // A hard-angled socket. The inner corner sits LOW and the outer rides
            // high - the opposite reads as a sad face rather than a furious one.
            Path2D socket = new Path2D.Double();
            socket.moveTo(innerX, eyeY - eyeH * 0.05);
            socket.lineTo(outerX, eyeY - eyeH * 0.80);
            socket.lineTo(outerX, eyeY + eyeH * 0.55);
            socket.lineTo(innerX, eyeY + eyeH * 0.80);
            socket.closePath();
            g2.setColor(ink);
            g2.fill(socket);

            // A hot coal burning inside it.
            double pw = eyeW * (mood == FACE_RAGE ? 0.40 : fierce ? 0.34 : smug ? 0.22 : 0.28);
            double px = ex + pupilShift - side * eyeW * 0.04;
            Paint oldP = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(px, eyeY + eyeH * 0.05), (float) (pw * 1.6),
                new float[]{0f, 0.45f, 1f},
                new Color[]{ Color.WHITE, glow, new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0) }));
            g2.fill(new Ellipse2D.Double(px - pw, eyeY + eyeH * 0.05 - pw, pw * 2, pw * 2));
            g2.setPaint(oldP);
        }

        // Heavy brows, angled down toward the nose. Always scowling; worse when
        // it flares, and worst of all when the player has just scored on it.
        double browDrop = mood == FACE_RAGE ? 0.82 : fierce ? 0.62 : 0.34;
        for (int side = -1; side <= 1; side += 2) {
            double ex = cx + side * eyeDX;
            double innerX = ex - side * eyeW * 0.78;
            double outerX = ex + side * eyeW * 0.84;
            // Gloating lifts one brow instead of dropping both.
            double drop = smug && side < 0 ? -0.45 : browDrop;
            Path2D brow = new Path2D.Double();
            brow.moveTo(innerX, eyeY - eyeH * (0.80 - drop * 0.60));
            brow.lineTo(outerX, eyeY - eyeH * (1.75 + drop * 0.30));
            brow.lineTo(outerX, eyeY - eyeH * (1.05 + drop * 0.30));
            brow.lineTo(innerX, eyeY - eyeH * (0.10 - drop * 0.60));
            brow.closePath();
            g2.setColor(ink);
            g2.fill(brow);
        }

        double mouthY = cy + r * 0.40;
        g2.setColor(ink);
        if (mood == FACE_SPEAKING) {
            double open = r * (0.16 + 0.12 * Math.abs(Math.sin(game.flameFlicker * 1.9)));
            double w = r * 0.50;
            Path2D maw = new Path2D.Double();
            maw.moveTo(cx - w / 2, mouthY - open * 0.5);
            maw.quadTo(cx, mouthY - open * 0.9, cx + w / 2, mouthY - open * 0.5);
            maw.quadTo(cx, mouthY + open * 1.1, cx - w / 2, mouthY - open * 0.5);
            maw.closePath();
            g2.fill(maw);
        } else if (smug) {
            // A lopsided grin, curling up on one side.
            double w = r * 0.58, h = r * 0.15;
            Path2D grin = new Path2D.Double();
            grin.moveTo(cx - w / 2, mouthY + h * 0.55);
            grin.quadTo(cx + w * 0.10, mouthY + h * 0.95, cx + w / 2, mouthY - h * 1.15);
            grin.quadTo(cx + w * 0.05, mouthY + h * 0.20, cx - w / 2, mouthY + h * 0.05);
            grin.closePath();
            g2.fill(grin);
        } else if (fierce) {
            // A wide jagged maw, thrown wider still when it is furious.
            double w = r * (mood == FACE_RAGE ? 0.76 : 0.66), h = r * (mood == FACE_RAGE ? 0.40 : 0.30);
            Path2D jaw = new Path2D.Double();
            jaw.moveTo(cx - w / 2, mouthY - h / 2);
            for (int i = 1; i <= 8; i++) {
                double t = i / 8.0;
                jaw.lineTo(cx - w / 2 + w * t, mouthY - h / 2 + (i % 2 == 0 ? 0 : h * 0.46));
            }
            jaw.lineTo(cx + w / 2, mouthY + h * 0.62);
            for (int i = 7; i >= 1; i--) {
                double t = i / 8.0;
                jaw.lineTo(cx - w / 2 + w * t, mouthY + h * 0.62 - (i % 2 == 0 ? 0 : h * 0.40));
            }
            jaw.lineTo(cx - w / 2, mouthY + h * 0.62);
            jaw.closePath();
            g2.fill(jaw);
        } else {
            // A closed, toothy grin - menacing even when it is only watching.
            double w = r * 0.60, h = r * 0.16;
            Path2D grin = new Path2D.Double();
            grin.moveTo(cx - w / 2, mouthY - h * 0.30);
            grin.quadTo(cx, mouthY + h * 0.55, cx + w / 2, mouthY - h * 0.30);
            for (int i = 7; i >= 1; i--) {
                double t = i / 8.0;
                grin.lineTo(cx - w / 2 + w * t, mouthY - h * 0.30 + (i % 2 == 0 ? 0 : h * 0.62)
                    + Math.sin(t * Math.PI) * h * 0.30);
            }
            grin.closePath();
            g2.fill(grin);
        }
    }

    /**
     * The looming star during the boss level. It sits clear of the HUD and the
     * question panel so its face stays readable while you play.
     * disc, so the final level feels like flying alongside something enormous.
     * It swells while a flare charges and blazes white when one erupts.
     */
    void drawStarBackdrop(Graphics2D g2) {
        double pulse = 0.5 + 0.5 * Math.sin(game.flameFlicker * 0.9);
        double charge = game.starFlareCharge > 0 ? 1 - clamp(game.starFlareCharge / FLARE_CHARGE_TIME, 0, 1) : 0;
        double burn = game.starFlareActive > 0 ? clamp(game.starFlareActive / FLARE_ACTIVE_TIME, 0, 1) : 0;

        // Kept right of the question panel's edge so the whole face stays clear of it.
        double cx = GAME_WIDTH - 72, cy = 198;
        // It closes in as the gauntlet is worked through, so the last questions
        // are fought with the thing filling the corner of the screen.
        double r = 74 + planetApproach() * 30 + pulse * 5 + charge * 14 + burn * 18;
        Paint old = g2.getPaint();

        // Corona bleeding across the field, brighter as a flare builds.
        int coronaAlpha = (int) (95 + charge * 70 + burn * 90);
        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(cx, cy), (float) (r * 3.4),
            new float[]{0f, 0.3f, 1f},
            new Color[]{
                new Color(255, 226, 150, coronaAlpha),
                new Color(255, 186, 86, (int) (coronaAlpha * 0.42)),
                new Color(255, 160, 60, 0)
            }));
        g2.fill(new Ellipse2D.Double(cx - r * 3.4, cy - r * 3.4, r * 6.8, r * 6.8));
        g2.setPaint(old);

        // Prominence loops arcing off the limb.
        AffineTransform oldTx = g2.getTransform();
        g2.translate(cx, cy);
        g2.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 5; i++) {
            double ang = Math.PI * 0.52 + i * 0.30 + Math.sin(game.flameFlicker * 0.12 + i) * 0.05;
            double reach = r * (0.30 + 0.10 * ((i * 7) % 3)) * (1 + charge * 0.7 + burn * 1.1);
            double sx = Math.cos(ang) * r, sy = Math.sin(ang) * r;
            double ex = Math.cos(ang + 0.34) * r, ey = Math.sin(ang + 0.34) * r;
            double mx = Math.cos(ang + 0.17) * (r + reach), my = Math.sin(ang + 0.17) * (r + reach);
            Path2D loop = new Path2D.Double();
            loop.moveTo(sx, sy);
            loop.quadTo(mx, my, ex, ey);
            g2.setColor(new Color(255, 190, 90, (int) (85 + burn * 120)));
            g2.draw(loop);
        }
        g2.setStroke(new BasicStroke(1f));
        g2.setTransform(oldTx);

        // The surface itself.
        Color core = burn > 0 ? Color.WHITE : new Color(255, 250, 235);
        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(cx - r * 0.35, cy - r * 0.25), (float) (r * 1.15),
            new float[]{0f, 0.55f, 1f},
            new Color[]{ core, new Color(255, 228, 140), new Color(255, 156, 46) }));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.setPaint(old);

        // Granulation, so the surface is not a flat disc.
        Shape clip = g2.getClip();
        g2.clip(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        for (int i = 0, n = game.graphics.count(26); i < n; i++) {
            double seed = i * 53.7;
            double a = seed % (Math.PI * 2);
            double d = (seed % r) * 0.9;
            double s = 10 + (i % 5) * 7;
            double bx = cx + Math.cos(a) * d, by = cy + Math.sin(a) * d + Math.sin(game.flameFlicker * 0.3 + i) * 4;
            g2.setColor(new Color(255, 205, 110, 42));
            g2.fill(new Ellipse2D.Double(bx - s / 2, by - s / 2, s, s));
        }
        g2.setClip(clip);

        // It watches you fly, scowls before a flare, and reacts to how you answer.
        int mood = game.starReaction > 0 ? game.starReactionMood
            : (game.starFlareActive > 0 || game.starFlareCharge > 0) ? FACE_FIERCE
            : FACE_CALM;
        drawStarFace(g2, cx, cy, r, mood, game.rocketX);
    }

    double planetApproach() {
        // The star swells as the gauntlet is worked through.
        if (game.atStar()) return clamp((game.starQuestionNumber() - 1) / (double) (STAR_QUESTIONS - 1), 0, 1);
        int start = PLANET_START_LEVEL[game.planetIndex()];
        int nextStart = Integer.MAX_VALUE;
        for (int s : PLANET_START_LEVEL) if (s > start && s < nextStart) nextStart = s;
        // The last planet runs up to the star, wherever the star currently is.
        int end = nextStart == Integer.MAX_VALUE ? STAR_LEVEL - 1 : nextStart - 1;
        int span = Math.max(1, end - start);
        return clamp((game.level - start) / (double) span, 0, 1);
    }

    void drawPlanetBackdrop(Graphics2D g2) {
        if (game.atStar()) {
            drawStarBackdrop(g2);
            return;
        }
        Planet p = game.currentPlanet();
        int cx = GAME_WIDTH - 40, cy = 192;
        int r = (int) (24 + planetApproach() * 34);

        Paint old = g2.getPaint();
        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(cx, cy), (float) (r * 1.9),
            new float[]{0f, 0.6f, 1f},
            new Color[]{
                new Color(p.accent.getRed(), p.accent.getGreen(), p.accent.getBlue(), 55),
                new Color(p.accent.getRed(), p.accent.getGreen(), p.accent.getBlue(), 18),
                new Color(p.accent.getRed(), p.accent.getGreen(), p.accent.getBlue(), 0)
            }));
        g2.fill(new Ellipse2D.Double(cx - r * 1.9, cy - r * 1.9, r * 3.8, r * 3.8));
        g2.setPaint(old);

        if (p.ringed) {
            double tilt = ringTilt(game.planetIndex());
            g2.setColor(new Color(p.accent.getRed(), p.accent.getGreen(), p.accent.getBlue(), 90));
            g2.rotate(tilt, cx, cy);
            g2.fill(new Ellipse2D.Double(cx - r * 1.7, cy - r * 0.3, r * 3.4, r * 0.6));
            g2.setColor(new Color(20, 16, 12, 110));
            g2.draw(new Ellipse2D.Double(cx - r * 1.7, cy - r * 0.3, r * 3.4, r * 0.6));
            g2.rotate(-tilt, cx, cy);
        }

        g2.setColor(p.base);
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        drawPlanetFeatures(g2, game.planetIndex(), cx, cy, r, p.base, p.accent);
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fill(new Ellipse2D.Double(cx - r * 0.15, cy - r, r * 1.15, r * 2));

        g2.setColor(new Color(190, 205, 225, 70));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    static double ringTilt(int planetIdx) { return planetIdx == 6 ? 1.30 : -0.35; }

    void drawPlanetFeatures(Graphics2D g2, int idx, double cx, double cy, double r, Color base, Color accent) {
        Shape oldClip = g2.getClip();
        g2.clip(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        Color shade = lerpColor(base, Color.BLACK, 0.25f);
        Color pale = lerpColor(base, Color.WHITE, 0.55f);

        switch (idx) {
            case 0 -> {
                g2.setColor(accent);
                double[][] craters = { {-0.35, -0.3, 0.3}, {0.3, 0.1, 0.38}, {-0.1, 0.45, 0.26}, {0.45, -0.45, 0.2}, {0.05, -0.15, 0.16} };
                for (double[] c : craters) {
                    double d = c[2] * r;
                    g2.fill(new Ellipse2D.Double(cx + c[0] * r - d / 2, cy + c[1] * r - d / 2, d, d));
                }
            }
            case 1 -> {
                g2.setColor(accent);
                for (int i = -2; i <= 2; i++) {
                    g2.fill(new Ellipse2D.Double(cx - r * 1.1, cy + i * r * 0.36 - r * 0.11, r * 2.2, r * 0.22));
                }
                g2.setColor(new Color(255, 245, 210, 70));
                g2.fill(new Ellipse2D.Double(cx - r * 1.1, cy - r * 0.55, r * 2.2, r * 0.3));
            }
            case 2 -> {
                g2.setColor(accent);
                g2.fill(new Ellipse2D.Double(cx - r * 0.75, cy - r * 0.55, r * 0.8, r * 0.6));
                g2.fill(new Ellipse2D.Double(cx - r * 0.1, cy - r * 0.05, r * 0.65, r * 0.75));
                g2.fill(new Ellipse2D.Double(cx + r * 0.3, cy - r * 0.7, r * 0.45, r * 0.35));
                g2.setColor(new Color(255, 255, 255, 190));
                g2.fill(new Ellipse2D.Double(cx - r, cy - r * 1.05, r * 2, r * 0.32));
                g2.fill(new Ellipse2D.Double(cx - r, cy + r * 0.75, r * 2, r * 0.32));
                g2.setColor(new Color(255, 255, 255, 110));
                g2.fill(new Ellipse2D.Double(cx - r * 0.9, cy - r * 0.18, r * 1.3, r * 0.22));
            }
            case 3 -> {
                g2.setColor(accent);
                g2.fill(new Ellipse2D.Double(cx - r * 0.7, cy - r * 0.1, r * 0.85, r * 0.45));
                g2.fill(new Ellipse2D.Double(cx + r * 0.1, cy + r * 0.3, r * 0.55, r * 0.3));
                g2.setColor(new Color(245, 248, 255, 215));
                g2.fill(new Ellipse2D.Double(cx - r * 0.45, cy - r * 1.08, r * 0.9, r * 0.38));
                g2.fill(new Ellipse2D.Double(cx - r * 0.3, cy + r * 0.82, r * 0.6, r * 0.28));
            }
            case 4 -> {
                for (int i = -3; i <= 3; i++) {
                    g2.setColor(i % 2 == 0 ? accent : shade);
                    g2.fill(new Ellipse2D.Double(cx - r * 1.1, cy + i * r * 0.28 - r * 0.1, r * 2.2, r * 0.2));
                }
                g2.setColor(new Color(200, 85, 60));
                g2.fill(new Ellipse2D.Double(cx - r * 0.1, cy + r * 0.18, r * 0.58, r * 0.34));
                g2.setColor(new Color(150, 55, 40, 160));
                g2.draw(new Ellipse2D.Double(cx - r * 0.1, cy + r * 0.18, r * 0.58, r * 0.34));
            }
            case 5 -> {
                for (int i = -2; i <= 2; i++) {
                    g2.setColor(i % 2 == 0 ? accent : lerpColor(base, Color.WHITE, 0.2f));
                    g2.fill(new Ellipse2D.Double(cx - r * 1.1, cy + i * r * 0.34 - r * 0.12, r * 2.2, r * 0.24));
                }
            }
            case 6 -> {
                g2.setColor(pale);
                g2.fill(new Ellipse2D.Double(cx - r * 1.1, cy - r * 0.4, r * 2.2, r * 0.3));
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fill(new Ellipse2D.Double(cx - r * 0.5, cy - r * 0.9, r * 1.0, r * 0.5));
            }
            default -> {
                g2.setColor(accent);
                g2.fill(new Ellipse2D.Double(cx - r * 1.1, cy - r * 0.15, r * 2.2, r * 0.26));
                g2.fill(new Ellipse2D.Double(cx - r * 1.1, cy + r * 0.45, r * 2.2, r * 0.18));
                g2.setColor(new Color(25, 35, 105));
                g2.fill(new Ellipse2D.Double(cx - r * 0.5, cy - r * 0.45, r * 0.6, r * 0.34));
                g2.setColor(new Color(235, 242, 255, 130));
                g2.fill(new Ellipse2D.Double(cx + r * 0.15, cy + r * 0.05, r * 0.45, r * 0.16));
            }
        }
        g2.setClip(oldClip);
    }

    static double orbitX(double f) { return f * GAME_WIDTH; }

    static double orbitY(double f) { return ORBIT_BASE_Y - Math.sin(f * Math.PI) * ORBIT_LIFT; }

    void drawPlanetTrack(Graphics2D g2) {
        // Nine stops: the eight planets, then the star at the end of the road.
        int n = planets.size() + 1;
        int current = destinationForLevel(game.level);

        Path2D orbit = new Path2D.Double();
        for (int i = 0; i <= 120; i++) {
            double f = i / 120.0;
            double x = orbitX(f), y = orbitY(f);
            if (i == 0) orbit.moveTo(x, y); else orbit.lineTo(x, y);
        }
        g2.setColor(new Color(150, 170, 200, 70));
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 7f}, 0f));
        g2.draw(orbit);
        g2.setStroke(new BasicStroke(1f));

        double travelled = clamp((game.level - 1) / (double) (STAR_LEVEL - 1), 0, 1);
        Path2D done = new Path2D.Double();
        int steps = Math.max(1, (int) (travelled * 120));
        for (int i = 0; i <= steps; i++) {
            double f = i / 120.0;
            double x = orbitX(f), y = orbitY(f);
            if (i == 0) done.moveTo(x, y); else done.lineTo(x, y);
        }
        g2.setColor(new Color(255, 215, 0, 110));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(done);
        g2.setStroke(new BasicStroke(1f));

        for (int i = 0; i < n; i++) {
            int dest = destinationAtSlot(i);
            boolean isStarNode = dest == STAR_DEST;
            double f = (i + 0.5) / n;
            double cx = orbitX(f), cy = orbitY(f);
            boolean isCurrent = dest == current;

            if (isStarNode) {
                boolean reachedStar = !game.isDestinationLocked(STAR_DEST);
                double sr = isCurrent ? 10 : 7;
                float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.5));
                if (isCurrent) {
                    g2.setColor(new Color(255, 235, 150, (int) (130 + pulse * 120)));
                    g2.fill(new Ellipse2D.Double(cx - sr - 5, cy - sr - 5, (sr + 5) * 2, (sr + 5) * 2));
                }
                Path2D spark = new Path2D.Double();
                for (int k = 0; k < 8; k++) {
                    double ang = k * Math.PI / 4 - Math.PI / 2;
                    double rad = k % 2 == 0 ? sr * 1.35 : sr * 0.5;
                    double px = cx + Math.cos(ang) * rad, py = cy + Math.sin(ang) * rad;
                    if (k == 0) spark.moveTo(px, py); else spark.lineTo(px, py);
                }
                spark.closePath();
                g2.setColor(reachedStar ? new Color(255, 226, 130) : new Color(110, 105, 90));
                g2.fill(spark);
                continue;
            }

            Planet p = planets.get(dest);
            boolean reached = !game.isPlanetLocked(dest);
            double r = isCurrent ? 10 : 7;

            if (isCurrent) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.5));
                g2.setColor(new Color(255, 215, 0, (int) (120 + pulse * 120)));
                g2.fill(new Ellipse2D.Double(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2));
            }
            g2.setColor(reached ? p.base : lerpColor(p.base, Color.DARK_GRAY, 0.7f));
            g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            if (p.ringed) {
                double tilt = ringTilt(dest);
                g2.setColor(new Color(p.accent.getRed(), p.accent.getGreen(), p.accent.getBlue(), reached ? 190 : 80));
                g2.rotate(tilt, cx, cy);
                g2.draw(new Ellipse2D.Double(cx - r * 1.7, cy - r * 0.32, r * 3.4, r * 0.64));
                g2.rotate(-tilt, cx, cy);
            }
            g2.setColor(reached ? Color.WHITE : new Color(150, 150, 150));
            g2.setFont(font(Font.BOLD, isCurrent ? 11 : 9));
            game.drawCentered(g2, String.valueOf(i + 1), (int) cx, (int) (cy + (isCurrent ? 4 : 3)));
        }

        double f = clamp(travelled, 0, 1);
        double mx = orbitX(f), my = orbitY(f) - 15;
        AffineTransform oldTx = g2.getTransform();
        g2.translate(mx, my);
        Path2D marker = new Path2D.Double();
        marker.moveTo(0, -6);
        marker.lineTo(4.5, 5);
        marker.lineTo(0, 2.5);
        marker.lineTo(-4.5, 5);
        marker.closePath();
        g2.setColor(new Color(255, 235, 150));
        g2.fill(marker);
        g2.setTransform(oldTx);
    }
}
