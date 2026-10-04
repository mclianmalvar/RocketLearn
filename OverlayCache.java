package rocketlearn;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Big gradient washes that hold still from frame to frame, painted once into
 * an image and then stamped. Filling a screen-sized gradient is slow - in
 * fullscreen without a graphics card the vignette and Venus's fog cost more
 * than the whole playfield - and stamping the same picture costs about half.
 *
 * Images are painted at the screen's own resolution and stamped pixel for
 * pixel, so what appears is exactly what painting the gradient would give.
 */
class OverlayCache {

    /** Few enough that a big screen's worth of images never piles up. */
    static final int MAX_IMAGES = 4;

    private static final class Stamp {
        final BufferedImage image;
        final double scaleX, scaleY;

        Stamp(BufferedImage image, double scaleX, double scaleY) {
            this.image = image;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }
    }

    private final Map<String, Stamp> images = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Stamp> eldest) { return size() > MAX_IMAGES; }
    };

    /**
     * Stamps the overlay named key over the game-unit rectangle (x, y, w, h),
     * painting it first when the key is new or the screen has been rescaled.
     * The painter draws in game units with the rectangle's corner at (0, 0).
     * The key must name everything the painting depends on.
     */
    void draw(Graphics2D g2, String key, double x, double y, int w, int h, Consumer<Graphics2D> painter) {
        AffineTransform tx = g2.getTransform();
        double sx = tx.getScaleX(), sy = tx.getScaleY();
        // Only a plain scaled view can be stamped pixel for pixel; anything else is painted as usual.
        if (tx.getShearX() != 0 || tx.getShearY() != 0 || sx <= 0 || sy <= 0) {
            Paint oldPaint = g2.getPaint();
            g2.translate(x, y);
            painter.accept(g2);
            g2.setTransform(tx);
            g2.setPaint(oldPaint);
            return;
        }

        Stamp e = images.get(key);
        if (e == null || e.scaleX != sx || e.scaleY != sy) {
            BufferedImage img = new BufferedImage((int) Math.ceil(w * sx), (int) Math.ceil(h * sy), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D ig = img.createGraphics();
            ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ig.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            ig.scale(sx, sy);
            painter.accept(ig);
            ig.dispose();
            e = new Stamp(img, sx, sy);
            images.put(key, e);
        }

        // Stamped in screen pixels, so the picture is copied rather than resampled.
        g2.setTransform(AffineTransform.getTranslateInstance(
            Math.round(tx.getTranslateX() + x * sx), Math.round(tx.getTranslateY() + y * sy)));
        g2.drawImage(e.image, 0, 0, null);
        g2.setTransform(tx);
    }

    /** How many overlays are held right now. */
    int size() { return images.size(); }
}
