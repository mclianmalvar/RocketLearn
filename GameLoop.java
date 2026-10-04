package rocketlearn;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

/**
 * The game's heartbeat: steps the game and paints it at a steady rate.
 *
 * It replaced a javax.swing.Timer set to 16 ms. That held the game near 60
 * frames a second whatever the monitor could show, and Windows rounds the
 * timer to its own coarse clock, so frames came at uneven gaps - measured at
 * 31 to 58 a second on a 165 Hz screen. This loop keeps its own time to the
 * nanosecond, sleeps until just short of each frame and waits out the rest,
 * because a sleep on Windows wakes up to a couple of milliseconds late.
 */
final class GameLoop {

    /** How long before a frame is due the loop stops sleeping and waits it out exactly. */
    static final long WAKE_EARLY_NANOS = 1_000_000L;

    private final RocketLearn game;

    /**
     * The one thread allowed to run frames, or null when stopped. Each thread
     * checks it is still this one, so a loop stopped and started again at
     * once can never leave the old thread running beside the new.
     */
    private volatile Thread thread;

    GameLoop(RocketLearn game) { this.game = game; }

    synchronized void start() {
        if (thread != null) return;
        Thread t = new Thread(this::run, "game-loop");
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    /** Stops after the frame in progress, if any. The tests stop it so they can step the game by hand. */
    synchronized void stop() {
        Thread t = thread;
        thread = null;
        if (t != null && t != Thread.currentThread()) t.interrupt();
    }

    /** True while the calling thread is still the loop's current one. */
    private boolean current() { return thread == Thread.currentThread(); }

    private void run() {
        long next = System.nanoTime();
        while (current()) {
            long frameStart = System.nanoTime();
            try {
                // One frame, stepped and painted on the event thread and waited
                // for, so frames never pile up behind a slow paint.
                SwingUtilities.invokeAndWait(game::frame);
            } catch (InterruptedException e) {
                return;
            } catch (InvocationTargetException e) {
                // One bad frame is reported the way any thread's crash is, not allowed to stop the game.
                Thread self = Thread.currentThread();
                self.getUncaughtExceptionHandler().uncaughtException(self, e.getCause());
            }
            int fps = game.targetFrameRate();
            // Unlimited: no waiting at all, the next frame starts at once. Input
            // still gets its turn: invokeAndWait queues each frame behind any
            // events already waiting, and this thread sleeps until it is done.
            if (fps <= 0) {
                next = System.nanoTime();
                continue;
            }
            long period = 1_000_000_000L / fps;
            long now = System.nanoTime();
            // In fullscreen the screen holds each frame until its next
            // refresh; waiting on top of that only misses refreshes. A frame
            // that came back in under half a period was not held, so the
            // screen is not really pacing and the loop's own clock takes over.
            if (game.screenPacesFrames() && now - frameStart >= period / 2) {
                next = now;
                continue;
            }
            next += period;
            // More than a frame behind - a slow machine, or the window being
            // dragged - starts the count again from now, instead of racing
            // through a burst of frames to catch up.
            if (now - next > period) next = now;
            if (!waitUntil(next)) return;
        }
    }

    /** Sleeps until just before the deadline, then waits out the rest exactly. False if stopped while waiting. */
    private boolean waitUntil(long deadline) {
        long sleepFor = deadline - System.nanoTime() - WAKE_EARLY_NANOS;
        if (sleepFor > 0) {
            try {
                Thread.sleep(sleepFor / 1_000_000L, (int) (sleepFor % 1_000_000L));
            } catch (InterruptedException e) {
                return false;
            }
        }
        while (current() && System.nanoTime() < deadline) Thread.onSpinWait();
        return current();
    }
}
