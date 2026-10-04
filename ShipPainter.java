package rocketlearn;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import static rocketlearn.RocketLearn.*;

/**
 * Draws the player's rocket in its chosen paint, including Starborn's halo
 * and spark trail, and the standing previews of it shown on menus.
 */
class ShipPainter {

    final RocketLearn game;

    ShipPainter(RocketLearn game) { this.game = game; }

    /** How many gold sparks a radiant ship sheds per second, and how fast boosting adds more. */
    static final double STARBORN_SPARK_RATE = 16, STARBORN_SPARK_BOOST = 20;

    /**
     * Starborn's trail: a steady drizzle of gold and white sparks from the
     * exhaust that drift down and fade, thicker while boosting.
     */
    void emitStarbornSparks(double x, double exhaustY, double shipW, double dt) {
        if (!game.currentSkin().radiant) return;
        double rate = STARBORN_SPARK_RATE + Math.max(0, game.throttle) * STARBORN_SPARK_BOOST;
        if (game.rng.nextDouble() >= rate * dt) return;
        Color c = game.rng.nextInt(3) == 0 ? new Color(255, 250, 225) : new Color(255, 205 + game.rng.nextInt(30), 80);
        game.particles.add(new Particle(
            x + (game.rng.nextDouble() - 0.5) * shipW * 0.5, exhaustY,
            (game.rng.nextDouble() - 0.5) * 40, 40 + game.rng.nextDouble() * 50,
            0.45 + game.rng.nextDouble() * 0.45, c, 0.1));
    }

    /** How many particles a trail sheds per second, and how much boosting adds. */
    static final double TRAIL_RATE = 18, TRAIL_BOOST = 22;

    /**
     * The chosen engine trail's particles, shed from the exhaust: dust that
     * sinks, mist that spreads, sparks that fly, or motes that shimmer and rise.
     */
    void emitTrail(double x, double exhaustY, double shipW, double dt) {
        EngineTrail trail = game.currentTrail();
        if (trail.kind == EngineTrail.Kind.NONE) return;
        double rate = TRAIL_RATE + Math.max(0, game.throttle) * TRAIL_BOOST;
        if (game.rng.nextDouble() >= rate * dt) return;
        java.util.Random r = game.rng;
        Color c = trail.particles[r.nextInt(trail.particles.length)];
        double px = x + (r.nextDouble() - 0.5) * shipW * 0.4;
        switch (trail.kind) {
            case DUST -> game.particles.add(new Particle(px, exhaustY, (r.nextDouble() - 0.5) * 30,
                30 + r.nextDouble() * 25, 0.7 + r.nextDouble() * 0.4, c, 0.05));
            case MIST -> game.particles.add(new Particle(px, exhaustY, (r.nextDouble() - 0.5) * 50,
                20 + r.nextDouble() * 20, 0.8 + r.nextDouble() * 0.4, c, -0.02));
            case SPARKS -> game.particles.add(new Particle(px, exhaustY, (r.nextDouble() - 0.5) * 120,
                60 + r.nextDouble() * 80, 0.25 + r.nextDouble() * 0.2, c, 0.4));
            case SHIMMER -> game.particles.add(new Particle(px, exhaustY, (r.nextDouble() - 0.5) * 40,
                20 + r.nextDouble() * 20, 0.6 + r.nextDouble() * 0.3, c, -0.15));
            default -> { }
        }
    }

    /**
     * Draws the rocket in the given paint at (x, y), standing still, without
     * disturbing the real ship's position or the player's chosen paint.
     */
    void drawShipPreview(Graphics2D g2, int skinIndex, double x, double y, double scale) {
        AffineTransform oldTx = g2.getTransform();
        g2.translate(x, y);
        g2.scale(scale, scale);
        double sx = game.rocketX, sy = game.rocketY, svx = game.rocketVX;
        int savedSkin = game.shipSkinIndex;
        game.rocketX = 0; game.rocketY = 0; game.rocketVX = 0;
        game.shipSkinIndex = skinIndex;
        drawRocket(g2);
        game.rocketX = sx; game.rocketY = sy; game.rocketVX = svx;
        game.shipSkinIndex = savedSkin;
        g2.setTransform(oldTx);
    }

    Path2D rocketBodyPath(double halfW, double halfH) {
        Path2D body = new Path2D.Double();
        body.moveTo(0, -halfH);
        body.lineTo(halfW, halfH * 0.5);
        body.lineTo(halfW * 0.5, halfH);
        body.lineTo(-halfW * 0.5, halfH);
        body.lineTo(-halfW, halfH * 0.5);
        body.closePath();
        return body;
    }

    void drawRocket(Graphics2D g2) {
        double halfW = game.rocketW / 2.0, halfH = game.rocketH / 2.0;
        double tilt = clamp(game.rocketVX / game.rocketMaxSpeed(), -1, 1) * 0.3 + (game.state == State.PLAYING ? game.windLean : 0);
        ShipSkin skin = game.currentSkin();

        AffineTransform oldTx = g2.getTransform();
        g2.translate(game.rocketX, game.rocketY + game.recoil * 4);
        g2.rotate(tilt);

        if (skin.radiant) drawRadiantHalo(g2, halfW, halfH);

        if (game.muzzleFlash > 0) {
            float a = (float) clamp(game.muzzleFlash / 0.08, 0, 1);
            if (game.graphics.glows) {
                Paint oldPaint = g2.getPaint();
                g2.setPaint(new RadialGradientPaint(
                    new Point2D.Double(0, -halfH - 4), 16 + a * 10,
                    new float[]{0f, 1f},
                    new Color[]{ withAlpha(skin.laserGlow, (int) (a * 220)), withAlpha(skin.laserGlow, 0) }));
                g2.fill(new Ellipse2D.Double(-26, -halfH - 30, 52, 52));
                g2.setPaint(oldPaint);
            }
            // Void's core is dark, so its flash takes the glow instead.
            Color flash = brightness(skin.laserCore) < 0.4 ? skin.laserGlow : lerpColor(skin.laserCore, Color.WHITE, 0.5f);
            g2.setColor(withAlpha(flash, (int) (a * 235)));
            Path2D spark = new Path2D.Double();
            spark.moveTo(0, -halfH - 13 * a - 6);
            spark.lineTo(5 * a, -halfH - 2);
            spark.lineTo(0, -halfH + 2);
            spark.lineTo(-5 * a, -halfH - 2);
            spark.closePath();
            g2.fill(spark);
        }

        drawNozzle(g2, halfW, halfH);

        double boostAmt = Math.max(0, game.throttle), slowAmt = Math.max(0, -game.throttle);
        double flameScale = 1.0 + boostAmt * 1.4 - slowAmt * 0.6;
        double flickerRate = 1.0 + boostAmt * 1.5 - slowAmt * 0.6;
        double flameWidthScale = 1.0 + boostAmt * 0.35 - slowAmt * 0.25;
        double flameLen = (14 + Math.abs(Math.sin(game.flameFlicker * flickerRate)) * 10 + Math.abs(game.rocketVX) * 0.04) * flameScale;
        // A chosen trail burns its planet's colour; otherwise a radiant ship
        // burns the star's gold. Boost and brake still tint either.
        EngineTrail trail = game.currentTrail();
        Color idleFlame = trail.kind != EngineTrail.Kind.NONE ? trail.flame
            : skin.radiant ? new Color(255, 212, 80) : Color.ORANGE;
        Color outerFlameColor = boostAmt > 0 ? lerpColor(idleFlame, new Color(150, 225, 255), (float) boostAmt)
            : slowAmt > 0 ? lerpColor(idleFlame, new Color(95, 95, 115), (float) slowAmt) : idleFlame;
        Color coreFlameColor = lerpColor(new Color(255, 250, 200), new Color(255, 255, 255), (float) boostAmt);

        Path2D flameOuter = new Path2D.Double();
        flameOuter.moveTo(-halfW * 0.4 * flameWidthScale, halfH);
        flameOuter.lineTo(0, halfH + flameLen);
        flameOuter.lineTo(halfW * 0.4 * flameWidthScale, halfH);
        flameOuter.closePath();
        g2.setColor(outerFlameColor);
        g2.fill(flameOuter);
        Path2D flameCore = new Path2D.Double();
        flameCore.moveTo(-halfW * 0.18 * flameWidthScale, halfH);
        flameCore.lineTo(0, halfH + flameLen * 0.6);
        flameCore.lineTo(halfW * 0.18 * flameWidthScale, halfH);
        flameCore.closePath();
        g2.setColor(coreFlameColor);
        g2.fill(flameCore);

        // Each side burns its own colour, and a burn against the drift runs
        // longer and hotter, so on ice or in wind the player can read which
        // way the ship is actually being pushed.
        double sideFlicker = 8 + Math.abs(Math.sin(game.flameFlicker * 1.7)) * 6;
        int dir = game.steerDir();
        boolean braking = dir != 0 && Math.signum(game.rocketVX) == -dir && Math.abs(game.rocketVX) > 30;
        double jetLen = braking ? sideFlicker * 1.6 : sideFlicker;
        if (dir > 0) drawSideThruster(g2, -halfW, halfH * 0.25, -1, jetLen, THRUST_RIGHT_COLOR, braking);
        if (dir < 0) drawSideThruster(g2, halfW, halfH * 0.25, 1, jetLen, THRUST_LEFT_COLOR, braking);

        drawHull(g2, skin, halfW, halfH);

        g2.setTransform(oldTx);
    }

    // ---- the hull, painted once and stamped ---------------------------------
    //
    // The fins, hull, its artwork, the badges and the cockpit look the same
    // every frame for a given paint, so they are painted into an image once
    // and stamped; the flame, thrusters, halo and muzzle flash stay live.
    // The image is painted at twice the size it appears and stamped with
    // smooth scaling, so a tilted ship keeps clean edges.

    /** Hull images kept: enough for the ship in play plus a screen of previews. */
    static final int HULL_CACHE_SIZE = 16;
    /** How much finer than the screen the hull is painted, so tilting it stays crisp. */
    static final double HULL_OVERSAMPLE = 2.0;
    /** Past this many image pixels per game unit the hull is simply drawn live. */
    static final double HULL_MAX_RESOLUTION = 12.0;
    /** How far past the hull outline the image reaches, in game units, for the fins and outline strokes. */
    static final double HULL_PAD = 3;

    private record HullImage(java.awt.image.BufferedImage image, double resolution, double originX, double originY) { }

    final java.util.Map<String, HullImage> hullCache = new java.util.LinkedHashMap<>(HULL_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, HullImage> eldest) { return size() > HULL_CACHE_SIZE; }
    };

    /** Draws the fixed part of the ship centred on the current origin, from the cache when it can. */
    void drawHull(Graphics2D g2, ShipSkin skin, double halfW, double halfH) {
        AffineTransform tx = g2.getTransform();
        double screenScale = Math.sqrt(Math.abs(tx.getDeterminant()));
        double resolution = Math.ceil(screenScale * HULL_OVERSAMPLE * 4) / 4;   // quarter steps, so tiny changes reuse it
        if (!(resolution > 0) || resolution > HULL_MAX_RESOLUTION) {
            drawHullLive(g2, skin, halfW, halfH);
            return;
        }
        String key = ShipSkin.ALL.indexOf(skin) + "|" + badgeKey() + "|" + resolution + "|" + halfW + "x" + halfH;
        HullImage h = hullCache.get(key);
        if (h == null) {
            h = paintHull(skin, halfW, halfH, resolution);
            hullCache.put(key, h);
        }
        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform place = new AffineTransform();
        place.translate(-h.originX(), -h.originY());
        place.scale(1 / h.resolution(), 1 / h.resolution());
        g2.drawImage(h.image(), place, null);
        if (oldInterp != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
    }

    /** The badges the hull wears, as text: which planets are finished and which are gold. */
    String badgeKey() {
        boolean[] finished = game.rewards.finishedPlanets();
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < finished.length; p++) sb.append(!finished[p] ? '-' : game.planetRecords.gold(p) ? 'G' : 'F');
        return sb.toString();
    }

    private HullImage paintHull(ShipSkin skin, double halfW, double halfH, double resolution) {
        double originX = halfW * 1.15 + HULL_PAD, originY = halfH + HULL_PAD;
        int w = (int) Math.ceil(originX * 2 * resolution), hgt = (int) Math.ceil(originY * 2 * resolution);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, hgt, java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D ig = img.createGraphics();
        ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ig.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        ig.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        ig.scale(resolution, resolution);
        ig.translate(originX, originY);
        drawHullLive(ig, skin, halfW, halfH);
        ig.dispose();
        return new HullImage(img, resolution, originX, originY);
    }

    /** The fins, hull, its artwork and badges, and the cockpit, drawn as shapes. */
    void drawHullLive(Graphics2D g2, ShipSkin skin, double halfW, double halfH) {
        drawFin(g2, skin, halfW, halfH, -1);
        drawFin(g2, skin, halfW, halfH, 1);

        Path2D body = rocketBodyPath(halfW, halfH);
        g2.setPaint(new GradientPaint(
            (float) -halfW, 0, skin.hullLight,
            (float) halfW, 0, skin.hullDark));
        g2.fill(body);
        g2.setPaint(null);

        // Everything painted on the hull stays inside its outline.
        Shape oldClip = g2.getClip();
        g2.clip(body);
        drawPanelLines(g2, skin, halfW, halfH);
        drawDecal(g2, skin, halfW, halfH);
        drawNoseCone(g2, skin, halfW, halfH);
        drawHullBadges(g2, halfH);
        // A soft sheen down the lit side, so the hull reads as round rather than flat.
        g2.setPaint(new GradientPaint(
            (float) (-halfW * 0.7), 0, new Color(255, 255, 255, 110),
            (float) (-halfW * 0.15), 0, new Color(255, 255, 255, 0)));
        g2.fill(new Rectangle2D.Double(-halfW, -halfH, halfW, halfH * 2));
        g2.setPaint(null);
        g2.setClip(oldClip);

        g2.setColor(skin.trim.darker());
        g2.draw(body);

        drawCockpit(g2, skin, halfH);
    }

    static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clampInt(alpha, 0, 255));
    }

    static double brightness(Color c) {
        return (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) / 255.0;
    }

    /** A flared engine bell under the hull: dark metal with a lit rim, glowing inside when boosting. */
    void drawNozzle(Graphics2D g2, double halfW, double halfH) {
        double top = halfH - 1, bottom = halfH + 7;
        Path2D bell = new Path2D.Double();
        bell.moveTo(-halfW * 0.28, top);
        bell.lineTo(halfW * 0.28, top);
        bell.lineTo(halfW * 0.40, bottom);
        bell.lineTo(-halfW * 0.40, bottom);
        bell.closePath();
        g2.setPaint(new GradientPaint((float) (-halfW * 0.4), 0, new Color(120, 122, 132),
            (float) (halfW * 0.4), 0, new Color(40, 40, 46)));
        g2.fill(bell);
        g2.setPaint(null);
        g2.setColor(new Color(170, 172, 182));
        g2.draw(new Line2D.Double(-halfW * 0.40, bottom, halfW * 0.40, bottom));
        double heat = Math.max(0, game.throttle);
        g2.setColor(new Color(255, 170, 90, (int) (60 + heat * 150)));
        g2.fill(new Rectangle2D.Double(-halfW * 0.22, bottom - 2.5, halfW * 0.44, 2));
    }

    /** One fin, shaded from root to tip, with a lit leading edge and a band of the hull's trim. */
    void drawFin(Graphics2D g2, ShipSkin skin, double halfW, double halfH, int side) {
        Path2D fin = new Path2D.Double();
        fin.moveTo(side * halfW * 0.5, halfH * 0.25);
        fin.lineTo(side * halfW * 1.15, halfH * 0.95);
        fin.lineTo(side * halfW * 0.5, halfH);
        fin.closePath();
        g2.setPaint(new GradientPaint((float) (side * halfW * 0.5), (float) (halfH * 0.3), skin.finFill.brighter(),
            (float) (side * halfW * 1.1), (float) (halfH * 0.95), skin.finFill.darker()));
        g2.fill(fin);
        g2.setPaint(null);

        Shape oldClip = g2.getClip();
        g2.clip(fin);
        g2.setColor(withAlpha(skin.trim.brighter(), 170));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new Line2D.Double(side * halfW * 0.5, halfH * 0.78, side * halfW * 1.2, halfH * 0.78));
        g2.setStroke(new BasicStroke(1f));
        g2.setClip(oldClip);

        g2.setColor(skin.finLine);
        g2.draw(fin);
        g2.setColor(new Color(255, 255, 255, 120));
        g2.draw(new Line2D.Double(side * halfW * 0.55, halfH * 0.32, side * halfW * 1.08, halfH * 0.9));
    }

    /**
     * A sticker for every finished planet, in route order along the band by
     * the tail: the planet's own colour, ringed in gold where it earned gold.
     */
    void drawHullBadges(Graphics2D g2, double halfH) {
        boolean[] finished = game.rewards.finishedPlanets();
        List<Integer> earned = new ArrayList<>();
        for (int p : TRAVEL_ORDER) if (finished[p]) earned.add(p);
        if (earned.isEmpty()) return;
        double d = 2.4, step = 2.8, y = halfH * 0.91;
        double x0 = -(earned.size() - 1) * step / 2;
        for (int i = 0; i < earned.size(); i++) {
            int p = earned.get(i);
            Planet planet = Planet.ALL.get(p);
            double cx = x0 + i * step;
            g2.setColor(game.planetRecords.gold(p) ? new Color(255, 210, 70) : new Color(30, 30, 40, 200));
            g2.fill(new Ellipse2D.Double(cx - d / 2 - 0.5, y - d / 2 - 0.5, d + 1, d + 1));
            g2.setColor(planet.base);
            g2.fill(new Ellipse2D.Double(cx - d / 2, y - d / 2, d, d));
        }
    }

    /** Faint seams and rivets where the hull plates meet, under the paint's artwork. */
    void drawPanelLines(Graphics2D g2, ShipSkin skin, double halfW, double halfH) {
        g2.setColor(withAlpha(skin.trim, 150));
        g2.draw(new Line2D.Double(-halfW, halfH * 0.35, halfW, halfH * 0.35));
        g2.draw(new Line2D.Double(-halfW, halfH * 0.82, halfW, halfH * 0.82));
        g2.setColor(withAlpha(skin.trim.brighter(), 190));
        for (double t = -1; t <= 1.01; t += 0.5) {
            g2.fill(new Ellipse2D.Double(t * halfW * 0.55 - 1.1, halfH * 0.35 - 1.1, 2.2, 2.2));
        }
    }

    /** The tip of the hull in the trim colour, with a small bright cap. */
    void drawNoseCone(Graphics2D g2, ShipSkin skin, double halfW, double halfH) {
        double coneBottom = -halfH * 0.5;
        g2.setPaint(new GradientPaint((float) -halfW, 0, skin.trim.brighter(), (float) halfW, 0, skin.trim.darker()));
        g2.fill(new Rectangle2D.Double(-halfW, -halfH, halfW * 2, coneBottom + halfH));
        g2.setPaint(null);
        g2.setColor(withAlpha(skin.trim.darker().darker(), 200));
        g2.draw(new Line2D.Double(-halfW, coneBottom, halfW, coneBottom));
        g2.setColor(new Color(255, 255, 255, 150));
        g2.fill(new Ellipse2D.Double(-2, -halfH + 1, 4, 6));
    }

    /** The glass: a lit frame in the hull's trim, tinted glass with depth, and a curved reflection. */
    void drawCockpit(Graphics2D g2, ShipSkin skin, double halfH) {
        double cw = 15, cy = -halfH * 0.1;
        Ellipse2D.Double frame = new Ellipse2D.Double(-cw / 2 - 2.5, cy - cw / 2 - 2.5, cw + 5, cw + 5);
        g2.setPaint(new GradientPaint((float) (-cw / 2), (float) (cy - cw / 2), lerpColor(skin.trim, Color.WHITE, 0.45f),
            (float) (cw / 2), (float) (cy + cw / 2), skin.trim.darker()));
        g2.fill(frame);
        g2.setPaint(null);
        g2.setColor(withAlpha(skin.trim.darker().darker(), 220));
        g2.draw(frame);

        Ellipse2D.Double cockpit = new Ellipse2D.Double(-cw / 2, cy - cw / 2, cw, cw);
        Color glassLight = lerpColor(new Color(150, 210, 255), skin.laserGlow, 0.25f);
        Color glassDark = lerpColor(new Color(30, 70, 130), skin.laserGlow.darker().darker(), 0.25f);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(-cw * 0.15, cy - cw * 0.15), (float) (cw * 0.75),
            new float[]{0f, 1f}, new Color[]{ glassLight, glassDark }));
        g2.fill(cockpit);
        g2.setPaint(null);
        g2.setColor(new Color(15, 30, 55));
        g2.draw(cockpit);

        // A curved highlight across the upper glass and a small glint.
        g2.setColor(new Color(255, 255, 255, 150));
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(-cw / 2 + 2, cy - cw / 2 + 2, cw - 4, cw - 4, 100, 70, Arc2D.OPEN));
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255, 255, 255, 210));
        g2.fill(new Ellipse2D.Double(-cw / 2 + 3, cy - cw / 2 + 3, 3.5, 3.5));
    }

    /**
     * Each paint's own artwork, drawn already clipped to the hull. Kept in the
     * lower two thirds and away from the cockpit so the ship reads at a glance.
     */
    void drawDecal(Graphics2D g2, ShipSkin skin, double halfW, double halfH) {
        Stroke oldStroke = g2.getStroke();
        Color accent = skin.finFill;
        switch (skin.decal) {
            case STRIPES -> {
                // Twin racing stripes from nose to tail.
                g2.setColor(withAlpha(accent, 210));
                g2.fill(new Rectangle2D.Double(-5.5, -halfH, 2.6, halfH * 2));
                g2.fill(new Rectangle2D.Double(2.9, -halfH, 2.6, halfH * 2));
            }
            case FLAMES -> {
                // Three tongues of flame licking up from the tail.
                double[][] tongues = { { -9, 0.15 }, { 0, -0.05 }, { 9, 0.15 } };
                for (double[] tongue : tongues) {
                    double x = tongue[0], tip = halfH * tongue[1];
                    Path2D flame = new Path2D.Double();
                    flame.moveTo(x - 6, halfH);
                    flame.quadTo(x - 6, halfH * 0.55, x - 1, tip + 6);
                    flame.quadTo(x + 1, tip, x, tip);
                    flame.quadTo(x + 5, halfH * 0.45, x + 6, halfH);
                    flame.closePath();
                    g2.setPaint(new GradientPaint(0, (float) halfH, new Color(200, 70, 30, 230),
                        0, (float) tip, new Color(255, 200, 90, 230)));
                    g2.fill(flame);
                }
                g2.setPaint(null);
            }
            case SUNBURST -> {
                // A sun emblem with twelve rays.
                double cx = 0, cy = halfH * 0.62, r = 4.5;
                g2.setColor(withAlpha(accent, 220));
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6, len = i % 2 == 0 ? 9 : 6.5;
                    Path2D ray = new Path2D.Double();
                    ray.moveTo(cx + Math.cos(a - 0.2) * r, cy + Math.sin(a - 0.2) * r);
                    ray.lineTo(cx + Math.cos(a) * len, cy + Math.sin(a) * len);
                    ray.lineTo(cx + Math.cos(a + 0.2) * r, cy + Math.sin(a + 0.2) * r);
                    ray.closePath();
                    g2.fill(ray);
                }
                g2.setColor(new Color(255, 236, 160));
                g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                g2.setColor(skin.finLine);
                g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
            case WAVES -> {
                // Two rolling ocean bands.
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int band = 0; band < 2; band++) {
                    double y0 = halfH * (0.5 + band * 0.25);
                    Path2D wave = new Path2D.Double();
                    wave.moveTo(-halfW, y0);
                    for (double x = -halfW; x <= halfW; x += 2) wave.lineTo(x, y0 + Math.sin(x * 0.45 + band) * 2.2);
                    g2.setColor(band == 0 ? withAlpha(accent, 220) : withAlpha(new Color(90, 170, 230), 220));
                    g2.draw(wave);
                }
            }
            case WEATHERED -> {
                // Patches of bare, rusted metal and a few scratches.
                double[][] patches = { { -11, 6, 7, 5 }, { 6, 12, 8, 6 }, { -4, 20, 6, 4 }, { 9, -2, 4, 5 }, { -8, -9, 4, 3 } };
                for (double[] p : patches) {
                    g2.setColor(new Color(130, 60, 36, 150));
                    g2.fill(new Ellipse2D.Double(p[0], p[1], p[2], p[3]));
                    g2.setColor(new Color(90, 40, 24, 120));
                    g2.fill(new Ellipse2D.Double(p[0] + p[2] * 0.3, p[1] + p[3] * 0.3, p[2] * 0.45, p[3] * 0.45));
                }
                g2.setColor(new Color(255, 235, 220, 140));
                g2.draw(new Line2D.Double(-12, 14, -6, 10));
                g2.draw(new Line2D.Double(4, 4, 11, 1));
                g2.draw(new Line2D.Double(-3, 24, 2, 21));
            }
            case CHECKER -> {
                // A chequered band, like a racing flag.
                double size = 4, y0 = halfH * 0.42;
                for (int row = 0; row < 3; row++) {
                    for (int col = -6; col < 6; col++) {
                        g2.setColor((row + col) % 2 == 0 ? withAlpha(accent, 230) : new Color(250, 240, 220, 230));
                        g2.fill(new Rectangle2D.Double(col * size, y0 + row * size, size, size));
                    }
                }
            }
            case FILIGREE -> {
                // Gilded scrollwork curling out from the centre line.
                g2.setColor(new Color(205, 165, 70, 230));
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int side = -1; side <= 1; side += 2) {
                    g2.draw(new QuadCurve2D.Double(0, halfH * 0.3, side * 10, halfH * 0.35, side * 8, halfH * 0.6));
                    g2.draw(new QuadCurve2D.Double(side * 8, halfH * 0.6, side * 5, halfH * 0.72, side * 2.5, halfH * 0.6));
                    g2.draw(new QuadCurve2D.Double(0, halfH * 0.55, side * 7, halfH * 0.75, side * 4, halfH * 0.95));
                    g2.fill(new Ellipse2D.Double(side * 10 - 1.3, halfH * 0.35 - 1.3, 2.6, 2.6));
                }
                g2.fill(new Ellipse2D.Double(-1.8, halfH * 0.5 - 1.8, 3.6, 3.6));
            }
            case FROST -> {
                // Rime creeping up from the tail, and snowflakes on the shoulders.
                g2.setPaint(new GradientPaint(0, (float) halfH, new Color(255, 255, 255, 170),
                    0, (float) (halfH * 0.4), new Color(255, 255, 255, 0)));
                g2.fill(new Rectangle2D.Double(-halfW, halfH * 0.4, halfW * 2, halfH * 0.6));
                g2.setPaint(null);
                g2.setColor(new Color(255, 255, 255, 220));
                double[][] flakes = { { -10, 8, 3.2 }, { 10, 12, 2.6 }, { -3, 22, 2.2 }, { 6, 22, 1.8 } };
                for (double[] f : flakes) {
                    for (int i = 0; i < 3; i++) {
                        double a = i * Math.PI / 3;
                        g2.draw(new Line2D.Double(f[0] - Math.cos(a) * f[2], f[1] - Math.sin(a) * f[2],
                            f[0] + Math.cos(a) * f[2], f[1] + Math.sin(a) * f[2]));
                    }
                }
            }
            case STARFIELD -> {
                // A slice of deep space across the hull, scattered with stars.
                g2.setPaint(new GradientPaint(0, (float) (halfH * 0.2), new Color(20, 14, 60, 0),
                    0, (float) halfH, new Color(20, 14, 60, 220)));
                g2.fill(new Rectangle2D.Double(-halfW, halfH * 0.2, halfW * 2, halfH * 0.8));
                g2.setPaint(null);
                double[][] stars = { { -9, 10, 1.4 }, { 7, 14, 1.8 }, { -2, 19, 1.1 }, { 11, 22, 1.2 }, { -12, 22, 1.0 },
                    { 3, 26, 1.5 }, { -6, 27, 1.0 }, { 13, 4, 1.0 } };
                for (double[] s : stars) {
                    g2.setColor(new Color(235, 225, 255, 230));
                    g2.fill(new Ellipse2D.Double(s[0] - s[2] / 2, s[1] - s[2] / 2, s[2], s[2]));
                }
                g2.setColor(new Color(190, 140, 255, 200));
                g2.draw(new Arc2D.Double(-7, 16, 10, 6, 20, 250, Arc2D.OPEN));
            }
            case CHEVRONS -> {
                // Stacked gold chevrons pointing to the nose.
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 3; i++) {
                    double y = halfH * (0.45 + i * 0.2);
                    Path2D v = new Path2D.Double();
                    v.moveTo(-9, y + 5);
                    v.lineTo(0, y);
                    v.lineTo(9, y + 5);
                    g2.setColor(i == 0 ? new Color(255, 245, 200, 240) : new Color(200, 130, 30, 230));
                    g2.draw(v);
                }
            }
        }
        g2.setStroke(oldStroke);
    }

    /**
     * Starborn's light: a soft gold glow and eight slow-turning rays, the
     * star's own look in miniature. Kept faint so it never hides the answers.
     */
    void drawRadiantHalo(Graphics2D g2, double halfW, double halfH) {
        double pulse = 0.5 + 0.5 * Math.sin(game.flameFlicker * 0.9);
        double r = Math.max(halfW, halfH) * (1.35 + pulse * 0.12);

        // The turning rays alone still mark the ship as Starborn.
        if (game.graphics.glows) {
            Paint oldPaint = g2.getPaint();
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(0, 0), (float) r,
                new float[]{0f, 0.55f, 1f},
                new Color[]{
                    new Color(255, 225, 130, (int) (70 + pulse * 30)),
                    new Color(255, 200, 80, 30),
                    new Color(255, 190, 60, 0)
                }));
            g2.fill(new Ellipse2D.Double(-r, -r, r * 2, r * 2));
            g2.setPaint(oldPaint);
        }

        AffineTransform oldTx = g2.getTransform();
        g2.rotate(game.flameFlicker * 0.02);
        g2.setColor(new Color(255, 220, 120, (int) (28 + pulse * 22)));
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            double inner = r * 0.55, outer = r * (i % 2 == 0 ? 1.25 : 1.0);
            Path2D ray = new Path2D.Double();
            ray.moveTo(Math.cos(ang - 0.09) * inner, Math.sin(ang - 0.09) * inner);
            ray.lineTo(Math.cos(ang) * outer, Math.sin(ang) * outer);
            ray.lineTo(Math.cos(ang + 0.09) * inner, Math.sin(ang + 0.09) * inner);
            ray.closePath();
            g2.fill(ray);
        }
        g2.setTransform(oldTx);
    }

    /**
     * One shot with its head at (x, y), flying up, in the paint's laser style.
     * Shapes are about the size of the original bolt and centred on the same
     * point, which is all a hit is measured from - the look never changes play.
     * Animation runs off the shared flicker clock plus the shot's height, so
     * shots side by side do not pulse in step.
     */
    void drawLaser(Graphics2D g2, ShipSkin skin, double x, double y, boolean glows) {
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        AffineTransform oldTx = g2.getTransform();
        double t = game.flameFlicker + y * 0.05;
        Color core = skin.laserCore, glow = skin.laserGlow;

        // Trail and halo: soft glows on the higher settings, a plain streak on Low.
        double trailLen = skin.laser == ShipSkin.LaserShape.LANCE ? 36 : 28;
        if (glows) {
            g2.setPaint(new GradientPaint((float) x, (float) (y - 4), withAlpha(glow, 180),
                (float) x, (float) (y + trailLen), withAlpha(glow, 0)));
            g2.fill(new RoundRectangle2D.Double(x - 2, y - 4, 4, trailLen + 4, 4, 4));
            g2.setPaint(new RadialGradientPaint(new Point2D.Double(x, y - 2), 12f,
                new float[]{0f, 1f}, new Color[]{ withAlpha(glow, 130), withAlpha(glow, 0) }));
            g2.fill(new Ellipse2D.Double(x - 12, y - 14, 24, 24));
            g2.setPaint(null);
        } else {
            g2.setColor(withAlpha(glow, 120));
            g2.fill(new Rectangle2D.Double(x - 2, y - 4, 4, trailLen * 0.75));
        }

        switch (skin.laser) {
            case BOLT -> {
                g2.setColor(core);
                g2.fill(new RoundRectangle2D.Double(x - 3, y - 10, 6, 16, 4, 4));
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(x - 1, y - 9, 2, 10, 2, 2));
            }
            case FIREBALL -> {
                // A flickering fireball shedding embers.
                double flick = Math.sin(t * 3.1) * 1.5;
                Path2D tail = new Path2D.Double();
                tail.moveTo(x - 5, y - 2);
                tail.quadTo(x - 3 + flick, y + 10, x + flick, y + 18);
                tail.quadTo(x + 3 + flick, y + 10, x + 5, y - 2);
                tail.closePath();
                g2.setColor(withAlpha(glow, 220));
                g2.fill(tail);
                g2.fill(new Ellipse2D.Double(x - 5.5, y - 8, 11, 11));
                g2.setColor(core);
                g2.fill(new Ellipse2D.Double(x - 3.5, y - 6.5, 7, 7));
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(x - 1.5, y - 5, 3, 3));
                for (int i = 0; i < 3; i++) {
                    double ey = y + 10 + i * 7, ex = x + Math.sin(t * 2 + i * 2.1) * 4;
                    g2.setColor(withAlpha(i == 0 ? core : glow, 200 - i * 50));
                    g2.fill(new Ellipse2D.Double(ex - 1.2, ey - 1.2, 2.4, 2.4));
                }
            }
            case ORB -> {
                // A pulsing ball of sunlight with a thin corona.
                double r = 5.5 + Math.sin(t * 4) * 0.6;
                g2.setColor(withAlpha(glow, 230));
                g2.fill(new Ellipse2D.Double(x - r, y - 2 - r, r * 2, r * 2));
                g2.setColor(core);
                g2.fill(new Ellipse2D.Double(x - 3.2, y - 5.2, 6.4, 6.4));
                g2.setColor(withAlpha(core, 160));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new Ellipse2D.Double(x - r - 2, y - 4 - r, r * 2 + 4, r * 2 + 4));
            }
            case HELIX -> {
                // Two strands twisting round each other, like a wave rolling up.
                for (int strand = 0; strand < 2; strand++) {
                    for (int i = 0; i < 10; i++) {
                        double dy = i * 2.6, phase = t * 6 + i * 0.7 + strand * Math.PI;
                        double dx = Math.sin(phase) * 3.4, size = 2.6 - i * 0.18;
                        boolean front = Math.cos(phase) > 0;
                        g2.setColor(withAlpha(front ? core : glow, 230 - i * 18));
                        g2.fill(new Ellipse2D.Double(x + dx - size / 2, y - 6 + dy - size / 2, size, size));
                    }
                }
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(x - 2, y - 9, 4, 4));
            }
            case SLUG -> {
                // A heavy, banded slug with sparks flicking off its sides.
                g2.setColor(glow.darker());
                g2.fill(new RoundRectangle2D.Double(x - 4, y - 10, 8, 15, 6, 6));
                g2.setColor(core);
                g2.fill(new RoundRectangle2D.Double(x - 3, y - 10, 6, 7, 6, 6));
                g2.setColor(glow.darker().darker());
                g2.fill(new Rectangle2D.Double(x - 4, y - 1, 8, 1.6));
                double s = Math.sin(t * 5);
                g2.setColor(withAlpha(new Color(255, 220, 140), 220));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(x - 5, y + 2, x - 8 - s * 2, y + 6));
                g2.draw(new Line2D.Double(x + 5, y + 2, x + 8 + s * 2, y + 6));
            }
            case TEARDROP -> {
                // A drop of honey-gold light, point first.
                Path2D drop = new Path2D.Double();
                drop.moveTo(x, y - 12);
                drop.curveTo(x + 6, y - 4, x + 6, y + 4, x, y + 4);
                drop.curveTo(x - 6, y + 4, x - 6, y - 4, x, y - 12);
                drop.closePath();
                g2.setPaint(new GradientPaint((float) x, (float) (y - 12), core, (float) x, (float) (y + 4), glow));
                g2.fill(drop);
                g2.setPaint(null);
                g2.setColor(new Color(255, 255, 255, 210));
                g2.fill(new Ellipse2D.Double(x - 2.5, y - 4, 2.5, 4));
            }
            case LANCE -> {
                // A long, fine needle of pale light with a pointed tip.
                Path2D lance = new Path2D.Double();
                lance.moveTo(x, y - 16);
                lance.lineTo(x + 2.2, y - 9);
                lance.lineTo(x + 1.3, y + 14);
                lance.lineTo(x - 1.3, y + 14);
                lance.lineTo(x - 2.2, y - 9);
                lance.closePath();
                g2.setColor(core);
                g2.fill(lance);
                g2.setColor(withAlpha(glow, 220));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new Line2D.Double(x - 4, y - 7, x + 4, y - 7));
                g2.setColor(Color.WHITE);
                g2.fill(new Rectangle2D.Double(x - 0.5, y - 14, 1, 20));
            }
            case SHARD -> {
                // A spinning ice crystal with glints in its wake.
                g2.rotate(Math.sin(t * 3) * 0.35, x, y - 3);
                Path2D shard = new Path2D.Double();
                shard.moveTo(x, y - 13);
                shard.lineTo(x + 4.5, y - 3);
                shard.lineTo(x, y + 7);
                shard.lineTo(x - 4.5, y - 3);
                shard.closePath();
                g2.setPaint(new GradientPaint((float) (x - 4), (float) (y - 13), core, (float) (x + 4), (float) (y + 7), glow));
                g2.fill(shard);
                g2.setPaint(null);
                g2.setColor(new Color(255, 255, 255, 230));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new Line2D.Double(x, y - 12, x, y + 5));
                g2.draw(shard);
                g2.setTransform(oldTx);
                for (int i = 0; i < 3; i++) {
                    double gy = y + 11 + i * 7, gx = x + (i % 2 == 0 ? -2.5 : 2.5);
                    double a = 0.5 + 0.5 * Math.sin(t * 5 + i);
                    g2.setColor(new Color(255, 255, 255, (int) (80 + a * 150)));
                    g2.draw(new Line2D.Double(gx - 1.5, gy, gx + 1.5, gy));
                    g2.draw(new Line2D.Double(gx, gy - 1.5, gx, gy + 1.5));
                }
            }
            case VOID -> {
                // A pocket of darkness ringed in violet, its rim slowly turning.
                g2.setColor(withAlpha(glow, 230));
                g2.fill(new Ellipse2D.Double(x - 6, y - 9, 12, 12));
                g2.setColor(core);
                g2.fill(new Ellipse2D.Double(x - 4, y - 7, 8, 8));
                g2.setColor(new Color(235, 215, 255, 230));
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double spin = Math.toDegrees(t * 3);
                g2.draw(new Arc2D.Double(x - 5.5, y - 8.5, 11, 11, spin, 110, Arc2D.OPEN));
                g2.draw(new Arc2D.Double(x - 5.5, y - 8.5, 11, 11, spin + 180, 110, Arc2D.OPEN));
            }
            case STAR -> {
                // A turning five-pointed star trailing sparkles: the star's own gift.
                g2.rotate(t * 2.5, x, y - 3);
                Path2D star = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? 7.5 : 3.2;
                    if (i == 0) star.moveTo(x + Math.cos(a) * r, y - 3 + Math.sin(a) * r);
                    else star.lineTo(x + Math.cos(a) * r, y - 3 + Math.sin(a) * r);
                }
                star.closePath();
                g2.setColor(glow);
                g2.fill(star);
                g2.setColor(core);
                g2.fill(new Ellipse2D.Double(x - 2.2, y - 5.2, 4.4, 4.4));
                g2.setTransform(oldTx);
                for (int i = 0; i < 4; i++) {
                    double sy = y + 9 + i * 6, sx = x + Math.sin(t * 3 + i * 1.7) * 4;
                    g2.setColor(withAlpha(i % 2 == 0 ? Color.WHITE : glow, 210 - i * 40));
                    g2.fill(new Ellipse2D.Double(sx - 1.1, sy - 1.1, 2.2, 2.2));
                }
            }
        }
        g2.setTransform(oldTx);
        g2.setStroke(oldStroke);
        g2.setPaint(oldPaint);
    }

    /** Jet colours by the way they push the ship, not by the side they sit on. */
    static final Color THRUST_LEFT_COLOR = new Color(120, 205, 255);

    static final Color THRUST_RIGHT_COLOR = new Color(255, 170, 90);

    void drawSideThruster(Graphics2D g2, double baseX, double baseY, double dir, double len, Color color, boolean hot) {
        double halfBase = hot ? 5 : 4;
        Path2D jet = new Path2D.Double();
        jet.moveTo(baseX, baseY - halfBase);
        jet.lineTo(baseX + dir * len, baseY);
        jet.lineTo(baseX, baseY + halfBase);
        jet.closePath();
        g2.setColor(color);
        g2.fill(jet);
        // A white-hot core, shorter than the jet around it.
        Path2D core = new Path2D.Double();
        core.moveTo(baseX, baseY - halfBase * 0.45);
        core.lineTo(baseX + dir * len * 0.55, baseY);
        core.lineTo(baseX, baseY + halfBase * 0.45);
        core.closePath();
        g2.setColor(new Color(255, 250, 235, hot ? 235 : 170));
        g2.fill(core);
    }
}
