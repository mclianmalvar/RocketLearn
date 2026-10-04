package rocketlearn;

/**
 * How much the game draws each frame, so it stays smooth on slower machines.
 *
 * Only looks change between levels, never play: the fog, the flare warning,
 * the shield and everything else that tells the player something is drawn
 * at every level. What goes is resolution, the purely decorative glows, and
 * how thickly the backgrounds and particles are scattered.
 *
 * Edges stay smoothed at every level. Measured, Java's smoothed shape filling
 * was faster than its jagged one here, so turning it off bought nothing.
 */
enum GraphicsQuality {
    LOW("Low", 1.0, true, false, false, 0.4, 90,
        "Fastest - plain effects, sparse backgrounds, drawn at a lower resolution"),
    MEDIUM("Medium", 1.5, false, true, false, 0.7, 220,
        "Balanced - most effects, lighter backgrounds, slightly softer in fullscreen"),
    HIGH("High", Double.MAX_VALUE, false, true, true, 1.0, Integer.MAX_VALUE,
        "Everything at full detail and full screen resolution");

    final String label;
    /**
     * The most screen pixels drawn per game pixel. Past this - a big
     * fullscreen display - the frame is drawn smaller and stretched to fit,
     * which is where most of the time on a slow machine goes.
     */
    final double maxRenderScale;
    /**
     * Whether to still draw small when the graphics card cannot do the
     * stretch. Without it the stretch has to be blocky to be quick, which
     * is worth it only when speed matters more than sharpness.
     */
    final boolean stretchWithoutGpu;
    /** The soft glows, gradients and highlights that only decorate. */
    final boolean glows;
    /**
     * Screen-sized washes painted afresh every frame: each planet's faint
     * tint and the main menu's pulsing nebula. Each costs about as much as
     * the whole playfield in fullscreen, for an effect few would miss.
     */
    final boolean liveWashes;
    /** The share of background detail drawn: stars, motes, crystals and streaks. */
    final double detail;
    /** Particles kept alive at once; past this the oldest go first. */
    final int maxParticles;
    /** The Settings hint for this level. */
    final String hint;

    GraphicsQuality(String label, double maxRenderScale, boolean stretchWithoutGpu, boolean glows, boolean liveWashes,
                    double detail, int maxParticles, String hint) {
        this.label = label;
        this.maxRenderScale = maxRenderScale;
        this.stretchWithoutGpu = stretchWithoutGpu;
        this.glows = glows;
        this.liveWashes = liveWashes;
        this.detail = detail;
        this.maxParticles = maxParticles;
        this.hint = hint;
    }

    /** How many of a full-detail count to draw at this level - never fewer than one. */
    int count(int full) { return Math.max(1, (int) Math.round(full * detail)); }

    /** The next level up or down, wrapping round at either end. */
    GraphicsQuality step(int dir) {
        GraphicsQuality[] all = values();
        return all[((ordinal() + dir) % all.length + all.length) % all.length];
    }

    /**
     * The level to start a new player on, judged from this computer. Without
     * a graphics card doing the drawing even a modern processor struggles in
     * fullscreen, so that is the deciding test; a two-core machine with one
     * still gets Medium. Only used until the player picks a level themselves.
     *
     * The probe costs a couple of hundred milliseconds and its answer cannot
     * change while the game runs, so it is taken once and kept. A holder
     * class does the keeping: the JVM makes a second caller wait for the
     * first rather than probe twice, which lets launch run it in the background.
     */
    static GraphicsQuality recommended() { return Probe.RESULT; }

    private static final class Probe {
        static final GraphicsQuality RESULT = probe();
    }

    private static GraphicsQuality probe() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return HIGH;
        boolean gpu;
        try {
            java.awt.GraphicsConfiguration gc = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
            java.awt.image.VolatileImage probe = gc.createCompatibleVolatileImage(1, 1);
            gpu = probe.getCapabilities().isAccelerated();
            probe.flush();
        } catch (RuntimeException e) {
            return MEDIUM;   // could not tell: the safe middle
        }
        return recommended(gpu, Runtime.getRuntime().availableProcessors());
    }

    static GraphicsQuality recommended(boolean gpu, int cores) {
        if (!gpu) return LOW;
        return cores <= 2 ? MEDIUM : HIGH;
    }

    /** Reads a saved name back, falling back to High for anything unrecognised. */
    static GraphicsQuality parse(String name) {
        for (GraphicsQuality q : values()) if (q.name().equalsIgnoreCase(name == null ? "" : name.trim())) return q;
        return HIGH;
    }
}
