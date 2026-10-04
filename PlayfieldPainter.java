package rocketlearn;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

import static rocketlearn.RocketLearn.*;

/**
 * Draws what moves on the playfield: the answer asteroids, power-ups,
 * bullets, debris, particles, and the hit, shield and freeze effects.
 */
class PlayfieldPainter {

    final RocketLearn game;

    PlayfieldPainter(RocketLearn game) { this.game = game; }

    // Pens and fixed colours used every frame, made once rather than per rock, crater or shot.
    static final BasicStroke STROKE_1 = new BasicStroke(1f), STROKE_1_5 = new BasicStroke(1.5f), STROKE_1_6 = new BasicStroke(1.6f),
        STROKE_2 = new BasicStroke(2f), STROKE_2_5 = new BasicStroke(2.5f), STROKE_3 = new BasicStroke(3f), STROKE_3_5 = new BasicStroke(3.5f),
        STROKE_6_ROUND = new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    static final Color HINT_BAND = new Color(15, 20, 35, 200), ROCK_SHINE = new Color(255, 255, 255, 70),
        CRATER_SHADOW = new Color(0, 0, 0, 70), CRATER_RIM = new Color(255, 255, 255, 60),
        LABEL_BACK = new Color(10, 14, 22, 165), ARROW_EDGE = new Color(20, 20, 30);

    /** A rock colour's shades, worked out once per colour rather than several times per rock each frame. */
    private final java.util.Map<Color, Color> darkShade = new java.util.HashMap<>(), lightShade = new java.util.HashMap<>();
    Color darkOf(Color c) { return darkShade.computeIfAbsent(c, k -> k.darker().darker()); }
    Color lightOf(Color c) { return lightShade.computeIfAbsent(c, Color::brighter); }

    void drawDebris(Graphics2D g2) {
        for (Debris d : game.debris) {
            AffineTransform oldTx = g2.getTransform();
            g2.translate(d.x, d.y);
            g2.rotate(d.rotation);
            Path2D rock = new Path2D.Double();
            for (int i = 0; i < 7; i++) {
                double ang = i * Math.PI * 2 / 7;
                double rr = d.radius * (0.78 + ((i * 37) % 11) / 33.0);
                double px = Math.cos(ang) * rr, py = Math.sin(ang) * rr;
                if (i == 0) rock.moveTo(px, py); else rock.lineTo(px, py);
            }
            rock.closePath();
            g2.setColor(new Color(105, 96, 88));
            g2.fill(rock);
            g2.setColor(new Color(58, 52, 47));
            g2.setStroke(STROKE_1_6);
            g2.draw(rock);
            g2.setStroke(STROKE_1);
            g2.setColor(new Color(255, 255, 255, 45));
            g2.fill(new Ellipse2D.Double(-d.radius * 0.35, -d.radius * 0.45, d.radius * 0.4, d.radius * 0.3));
            g2.setTransform(oldTx);
        }
    }

    void drawOptions(Graphics2D g2) {
        g2.setFont(font(Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getHeight();
        boolean showHint = game.hintActive();
        for (AnswerOption o : game.options) {
            Path2D asteroid = buildAsteroidPath(o);

            if (showHint && o.correct) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 1.5));
                drawHintHalo(g2, o, pulse);
                // A dark band under the gold, so the outline still reads on a
                // yellow or orange rock where gold on its own blends in.
                g2.setStroke(STROKE_6_ROUND);
                g2.setColor(HINT_BAND);
                g2.draw(asteroid);
                g2.setStroke(STROKE_3);
                g2.setColor(new Color(255, 215, 0, (int) (140 + pulse * 115)));
                g2.draw(asteroid);
                g2.setStroke(STROKE_1);
            }

            Shape oldClip = g2.getClip();
            g2.clip(asteroid);
            g2.setPaint(new GradientPaint(
                (float) (o.x - CHIP_W / 2.0), (float) (o.y - CHIP_H / 2.0), lightOf(o.color),
                (float) (o.x + CHIP_W / 2.0), (float) (o.y + CHIP_H / 2.0), darkOf(o.color)));
            g2.fillRect((int) (o.x - CHIP_W / 2.0), (int) (o.y - CHIP_H / 2.0), CHIP_W, CHIP_H);
            g2.setPaint(null);
            drawCraters(g2, o);
            g2.setColor(ROCK_SHINE);
            g2.fill(new Ellipse2D.Double(o.x - CHIP_W * 0.24, o.y - CHIP_H * 0.28, 7, 5));
            g2.setClip(oldClip);

            g2.setColor(game.highContrast ? Color.WHITE : darkOf(o.color));
            g2.setStroke(game.highContrast ? STROKE_3 : STROKE_1_6);
            g2.draw(asteroid);
            g2.setStroke(STROKE_1);

            drawAsteroidLabel(g2, o, fm, lineH);
            if (showHint && o.correct) drawHintArrow(g2, o);
        }
    }

    /**
     * A pulsing light behind the hinted answer, so it stands out from the
     * other rocks by more than its edge. Soft with glows on, a plain
     * translucent disc without them.
     */
    void drawHintHalo(Graphics2D g2, AnswerOption o, float pulse) {
        double rx = CHIP_W * 0.8, ry = CHIP_H * 1.3;
        if (game.graphics.glows) {
            Paint old = g2.getPaint();
            AffineTransform oldTx = g2.getTransform();
            g2.translate(o.x, o.y);
            g2.scale(1, ry / rx);   // stretch a round gradient to the rock's wide shape
            g2.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) rx,
                new float[]{0f, 1f},
                new Color[]{ new Color(255, 240, 160, (int) (110 + pulse * 80)), new Color(255, 215, 0, 0) }));
            g2.fill(new Ellipse2D.Double(-rx, -rx, rx * 2, rx * 2));
            g2.setTransform(oldTx);
            g2.setPaint(old);
        } else {
            g2.setColor(new Color(255, 240, 160, (int) (45 + pulse * 45)));
            g2.fill(new Ellipse2D.Double(o.x - rx * 0.75, o.y - ry * 0.75, rx * 1.5, ry * 1.5));
        }
    }

    /**
     * A white arrow bobbing over the hinted answer. The gold outline alone is
     * a colour cue, and against an orange or yellow rock it is easy to miss -
     * more so for a colour-blind player - so the hint carries a shape too.
     */
    void drawHintArrow(Graphics2D g2, AnswerOption o) {
        double bob = Math.sin(game.flameFlicker * 0.9) * 4;
        double tipY = o.y - CHIP_H / 2.0 - 6 + bob;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(o.x, tipY);
        arrow.lineTo(o.x - 11, tipY - 14);
        arrow.lineTo(o.x - 4, tipY - 14);
        arrow.lineTo(o.x - 4, tipY - 24);
        arrow.lineTo(o.x + 4, tipY - 24);
        arrow.lineTo(o.x + 4, tipY - 14);
        arrow.lineTo(o.x + 11, tipY - 14);
        arrow.closePath();
        g2.setColor(Color.WHITE);
        g2.fill(arrow);
        g2.setStroke(STROKE_2);
        g2.setColor(ARROW_EDGE);
        g2.draw(arrow);
        g2.setStroke(STROKE_1);
    }

    void drawCraters(Graphics2D g2, AnswerOption o) {
        for (double[] c : o.craters) {
            double ox = c[0], oy = c[1], size = c[2];
            double rx = ox * Math.cos(o.rotation) - oy * Math.sin(o.rotation);
            double ry = ox * Math.sin(o.rotation) + oy * Math.cos(o.rotation);
            double px = o.x + rx * CHIP_W;
            double py = o.y + ry * CHIP_H;
            double d = size * CHIP_H;
            g2.setColor(darkOf(o.color));
            g2.fill(new Ellipse2D.Double(px - d / 2, py - d / 2, d, d));
            // The pit is enough to read as a crater; its shading is only polish.
            if (!game.graphics.glows) continue;
            g2.setColor(CRATER_SHADOW);
            g2.fill(new Ellipse2D.Double(px - d * 0.05, py - d * 0.05, d * 0.55, d * 0.55));
            g2.setColor(CRATER_RIM);
            g2.setStroke(new BasicStroke((float) Math.max(1, d * 0.12)));
            g2.draw(new Arc2D.Double(px - d / 2, py - d / 2, d, d, 100, 130, Arc2D.OPEN));
            g2.setStroke(STROKE_1);
        }
    }

    void drawAsteroidLabel(Graphics2D g2, AnswerOption o, FontMetrics fm, int lineH) {
        List<String> lines = game.wrapText(fm, o.text, CHIP_W - 20);
        for (int size : game.largeText ? LABEL_FONT_SIZES_LARGE : LABEL_FONT_SIZES) {
            g2.setFont(font(Font.BOLD, size));
            fm = g2.getFontMetrics();
            lineH = fm.getHeight();
            lines = game.wrapText(fm, o.text, CHIP_W - 20);
            if (lines.size() <= 2) break;
        }

        int lblW = 0;
        for (String ln : lines) lblW = Math.max(lblW, fm.stringWidth(ln));
        int lblH = lines.size() * lineH;
        int boxX = (int) (o.x - lblW / 2.0 - 7), boxY = (int) (o.y - lblH / 2.0 - 5);
        int boxW = lblW + 14, boxH = lblH + 10;

        g2.setColor(game.highContrast ? Color.BLACK : LABEL_BACK);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
        g2.setColor(game.highContrast ? Color.WHITE : new Color(255, 255, 255, 45));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        double startY = o.y - (lines.size() - 1) * lineH / 2.0 + fm.getAscent() / 2.5;
        for (int i = 0; i < lines.size(); i++) {
            g2.setColor(new Color(0, 0, 0, 200));
            game.drawCentered(g2, lines.get(i), (int) o.x + 1, (int) (startY + i * lineH) + 1);
            g2.setColor(Color.WHITE);
            game.drawCentered(g2, lines.get(i), (int) o.x, (int) (startY + i * lineH));
        }
    }

    Path2D buildAsteroidPath(AnswerOption o) {
        double rx = CHIP_W / 2.0, ry = CHIP_H / 2.0;
        int n = o.shapeR.length;
        double[] px = new double[n], py = new double[n];
        for (int i = 0; i < n; i++) {
            double ang = o.rotation + i * (Math.PI * 2 / n);
            px[i] = o.x + Math.cos(ang) * rx * o.shapeR[i];
            py[i] = o.y + Math.sin(ang) * ry * o.shapeR[i];
        }
        Path2D path = new Path2D.Double();
        double startX = (px[0] + px[n - 1]) / 2, startY = (py[0] + py[n - 1]) / 2;
        path.moveTo(startX, startY);
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            double midX = (px[i] + px[next]) / 2, midY = (py[i] + py[next]) / 2;
            path.quadTo(px[i], py[i], midX, midY);
        }
        path.closePath();
        return path;
    }

    void drawPowerUps(Graphics2D g2) {
        for (PowerUp pu : game.powerUps) {
            Color c = pu.color();
            double bobY = pu.y + Math.sin(pu.bob) * 3;
            double r = POWERUP_W / 2.0;

            float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2.0));
            if (game.graphics.glows) {
                Paint old = g2.getPaint();
                g2.setPaint(new RadialGradientPaint(
                    new Point2D.Double(pu.x, bobY), (float) (r * 1.9),
                    new float[]{0f, 1f},
                    new Color[]{ new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (70 + pulse * 70)), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0) }));
                g2.fill(new Ellipse2D.Double(pu.x - r * 1.9, bobY - r * 1.9, r * 3.8, r * 3.8));
                g2.setPaint(old);
            } else {
                // Still pulses, so a gem stays easy to spot without the soft glow.
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (35 + pulse * 35)));
                g2.fill(new Ellipse2D.Double(pu.x - r * 1.2, bobY - r * 1.2, r * 2.4, r * 2.4));
            }

            AffineTransform oldTx = g2.getTransform();
            g2.translate(pu.x, bobY);
            g2.rotate(pu.rotation);
            Path2D gem = new Path2D.Double();
            for (int i = 0; i < 6; i++) {
                double ang = i * Math.PI / 3;
                double px = Math.cos(ang) * r * 0.78, py = Math.sin(ang) * r * 0.78;
                if (i == 0) gem.moveTo(px, py); else gem.lineTo(px, py);
            }
            gem.closePath();
            g2.setColor(new Color(12, 18, 28, 220));
            g2.fill(gem);
            g2.setStroke(STROKE_2_5);
            g2.setColor(c);
            g2.draw(gem);
            g2.setStroke(STROKE_1);
            g2.setTransform(oldTx);

            g2.setFont(font(Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            game.drawCentered(g2, pu.label(), (int) pu.x, (int) (bobY + 4));
        }
    }

    /** Every shot in flight, in the look of the ship's paint. */
    void drawBullets(Graphics2D g2) {
        ShipSkin skin = game.currentSkin();
        for (Bullet b : game.bullets) game.ship.drawLaser(g2, skin, b.x, b.y, game.graphics.glows);
    }

    /** White silhouettes of asteroids for a few frames after they are struck. */
    void drawHitFlashes(Graphics2D g2) {
        for (HitFlash f : game.hitFlashes) {
            float a = (float) clamp(f.life / f.maxLife, 0, 1);
            g2.setColor(new Color(255, 255, 255, (int) (a * 230)));
            g2.fill(f.shape);
            g2.setStroke(STROKE_2_5);
            g2.setColor(new Color(190, 240, 255, (int) (a * 200)));
            g2.draw(f.shape);
            g2.setStroke(STROKE_1);
        }
    }

    /** A protective bubble, brightening sharply at the moment it absorbs a hit. */
    void drawShieldBubble(Graphics2D g2) {
        boolean breaking = game.shieldFlash > 0;
        if (!game.hasShield && !breaking) return;

        double burst = breaking ? clamp(game.shieldFlash / 0.45, 0, 1) : 0;
        double r = 34 + burst * 16;
        float pulse = (float) (0.5 + 0.5 * Math.sin(game.flameFlicker * 2.2));
        int alpha = breaking ? (int) (burst * 220) : (int) (60 + pulse * 45);

        Paint old = g2.getPaint();
        g2.setPaint(new RadialGradientPaint(
            new Point2D.Double(game.rocketX, game.rocketY), (float) r,
            new float[]{0f, 0.72f, 1f},
            new Color[]{
                new Color(90, 200, 255, 0),
                new Color(90, 200, 255, alpha / 3),
                new Color(150, 230, 255, alpha)
            }));
        g2.fill(new Ellipse2D.Double(game.rocketX - r, game.rocketY - r, r * 2, r * 2));
        g2.setPaint(old);

        g2.setStroke(breaking ? STROKE_3 : STROKE_2);
        g2.setColor(new Color(170, 235, 255, Math.min(255, alpha + 60)));
        g2.draw(new Ellipse2D.Double(game.rocketX - r, game.rocketY - r, r * 2, r * 2));
        g2.setStroke(STROKE_1);
    }

    /** Frost creeping in from the edges while the freeze power-up is active. */
    void drawFreezeOverlay(Graphics2D g2) {
        if (game.freezeTimer <= 0) return;
        double strength = clamp(game.freezeTimer / FREEZE_DURATION, 0, 1);

        // The frost wedges alone still say "frozen"; the icy wash behind them is polish.
        if (game.graphics.glows) {
            Paint old = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0, GAME_HEIGHT / 2.0), (float) (GAME_WIDTH * 0.62),
                new float[]{0f, 0.55f, 1f},
                new Color[]{
                    new Color(150, 225, 255, 0),
                    new Color(150, 225, 255, (int) (28 * strength)),
                    new Color(190, 240, 255, (int) (120 * strength))
                }));
            g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            g2.setPaint(old);
        }

        // Tapered crystals creeping inward from each edge, receding as time runs out.
        g2.setColor(new Color(170, 226, 255, (int) (78 * strength)));
        frostEdge(g2, 22, strength, true);
        frostEdge(g2, 13, strength, false);
        g2.setColor(new Color(235, 250, 255, (int) (95 * strength)));
        frostEdge(g2, 22, strength * 0.55, true);
        frostEdge(g2, 13, strength * 0.55, false);
    }

    /**
     * Draws a row of frost wedges along one pair of edges - top and bottom when
     * horizontal, left and right otherwise. Each wedge tapers to a point so the
     * effect reads as ice rather than as scratches on the screen.
     */
    void frostEdge(Graphics2D g2, int count, double strength, boolean horizontal) {
        double span = horizontal ? GAME_WIDTH : GAME_HEIGHT;
        double far = horizontal ? GAME_HEIGHT : GAME_WIDTH;
        double half = span / count / 2.0;

        for (int i = 0; i < count; i++) {
            double centre = (i + 0.5) * (span / count);
            double depth = (16 + ((i * 41) % 21)) * strength;
            double w = half * (0.55 + ((i * 29) % 9) / 18.0);

            for (int side = 0; side < 2; side++) {
                double base = side == 0 ? 0 : far;
                double tip = side == 0 ? depth : far - depth;
                Path2D wedge = new Path2D.Double();
                if (horizontal) {
                    wedge.moveTo(centre - w, base);
                    wedge.lineTo(centre, tip);
                    wedge.lineTo(centre + w, base);
                } else {
                    wedge.moveTo(base, centre - w);
                    wedge.lineTo(tip, centre);
                    wedge.lineTo(base, centre + w);
                }
                wedge.closePath();
                g2.fill(wedge);
            }
        }
    }

    /** Full-screen flashes: white-orange on detonation, red when a heart is lost. */
    void drawImpactFlashes(Graphics2D g2) {
        if (game.explosionFlash > 0) {
            float a = (float) clamp(game.explosionFlash / 0.14, 0, 1);
            g2.setColor(new Color(255, 225, 170, (int) (a * a * 170 * game.flashScale())));
            g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        }
        if (game.damageFlash > 0) {
            // Reduce Motion softens the red edge but keeps it readable: it is
            // the clearest sign a heart was just lost.
            float a = (float) (clamp(game.damageFlash / 0.55, 0, 1) * Math.max(game.flashScale(), 0.4));
            if (!game.graphics.glows) {
                // An even red wash: the same warning without a full-screen gradient.
                g2.setColor(new Color(255, 35, 35, (int) (a * 75)));
                g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                return;
            }
            Paint old = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(GAME_WIDTH / 2.0, GAME_HEIGHT / 2.0), (float) (GAME_WIDTH * 0.62),
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                    new Color(255, 40, 40, 0),
                    new Color(255, 40, 40, (int) (a * 40)),
                    new Color(255, 30, 30, (int) (a * 165))
                }));
            g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            g2.setPaint(old);
        }
    }

    void drawShockwave(Graphics2D g2) {
        double t = 1 - clamp(game.explodeTimer / 0.6, 0, 1);
        double r = 8 + t * 75;
        float alpha = (float) (1 - t);
        g2.setStroke(STROKE_3_5);
        g2.setColor(new Color(255, 210, 120, (int) (alpha * 200)));
        g2.draw(new Ellipse2D.Double(game.rocketX - r, game.rocketY - r, r * 2, r * 2));
        g2.setStroke(STROKE_1_5);
        g2.setColor(new Color(255, 255, 255, (int) (alpha * 140)));
        double r2 = r * 0.6;
        g2.draw(new Ellipse2D.Double(game.rocketX - r2, game.rocketY - r2, r2 * 2, r2 * 2));
        g2.setStroke(STROKE_1);
    }

    void drawParticles(Graphics2D g2) {
        boolean round = game.graphics.glows;
        for (Particle p : game.particles) {
            float alpha = (float) clamp(p.life / p.maxLife, 0, 1);
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (alpha * 255)));
            double s = 5 * alpha + 1;
            // A few pixels across and moving fast, a square spark looks the same as a round one.
            if (round) g2.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            else g2.fillRect((int) (p.x - s / 2), (int) (p.y - s / 2), (int) Math.ceil(s), (int) Math.ceil(s));
        }
    }
}
