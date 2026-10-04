package rocketlearn;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * All audio. Sound effects are synthesized as raw PCM at class-load time and
 * played on a small shared thread pool, so rapid fire never spawns a thread per
 * shot. Background music is generated the same way, so the game needs no audio
 * files on disk - though menu_music.wav, easy_music.wav, medium_music.wav,
 * hard_music.wav and boss_music.wav are streamed instead when present, and any
 * that are missing or will not play are listed on the main menu.
 */
class Sfx {

    static final int SR = 44100;
    static final AudioFormat FORMAT = new AudioFormat(SR, 16, 1, true, false);

    /**
     * Four daemon workers with a short queue. Sounds requested while the queue
     * is full are dropped rather than queued up, which keeps audio in step with
     * the action instead of lagging behind it.
     */
    private static final ThreadPoolExecutor PLAYERS = new ThreadPoolExecutor(
        1, 4, 5, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(8),
        r -> {
            Thread t = new Thread(r, "sfx");
            t.setDaemon(true);
            return t;
        },
        new ThreadPoolExecutor.DiscardPolicy());

    static void shutdown() {
        PLAYERS.shutdownNow();
        stopMusic();
    }

    // ---------------------------------------------------------------- tones

    static void writeSample(byte[] data, int index, double sample) {
        short s = (short) (Math.max(-1, Math.min(1, sample)) * Short.MAX_VALUE);
        data[index] = (byte) (s & 0xFF);
        data[index + 1] = (byte) ((s >> 8) & 0xFF);
    }

    static byte[] tone(double f0, double f1, double dur, double vol) {
        int n = (int) (dur * SR);
        byte[] data = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double freq = f0 + (f1 - f0) * (i / (double) n);
            double env = 1.0 - (i / (double) n);
            writeSample(data, i * 2, Math.sin(2 * Math.PI * freq * t) * vol * env);
        }
        return data;
    }

    static byte[] chime(double[] freqs, double noteDur, double vol) {
        int nPerNote = (int) (noteDur * SR);
        byte[] data = new byte[nPerNote * 2 * freqs.length];
        int off = 0;
        for (double f : freqs) {
            for (int i = 0; i < nPerNote; i++) {
                double t = i / (double) SR;
                double env = 1.0 - (i / (double) nPerNote);
                writeSample(data, off, Math.sin(2 * Math.PI * f * t) * vol * env);
                off += 2;
            }
        }
        return data;
    }

    static byte[] noise(double dur, double vol) {
        int n = (int) (dur * SR);
        byte[] data = new byte[n * 2];
        Random r = new Random();
        for (int i = 0; i < n; i++) {
            double env = Math.pow(1.0 - i / (double) n, 2);
            writeSample(data, i * 2, (r.nextDouble() * 2 - 1) * vol * env);
        }
        return data;
    }

    static final byte[] SHOOT = tone(1100, 350, 0.12, 0.22);
    static final byte[] CORRECT = chime(new double[]{523.25, 783.99}, 0.09, 0.28);
    static final byte[] HEAL = chime(new double[]{659.25, 987.77, 1318.5}, 0.09, 0.25);
    static final byte[] EXPLOSION = noise(0.4, 0.35);
    static final byte[] CLICK = tone(500, 500, 0.04, 0.15);
    static final byte[] CONFIRM = chime(new double[]{440, 660}, 0.06, 0.22);
    static final byte[] GAME_OVER_SOUND = tone(400, 120, 0.6, 0.3);
    static final byte[] VICTORY_SOUND = chime(new double[]{523.25, 659.25, 783.99, 1046.5}, 0.16, 0.3);

    /** The right answer lighting up: a soft rising arpeggio, unlike any hit or pickup. */
    static final byte[] HINT = chime(new double[]{587.33, 739.99, 880.00, 1174.66}, 0.07, 0.2);

    /** A dry tick for the last few seconds of the timer. */
    static final byte[] TICK = tone(1500, 900, 0.045, 0.18);
    /** A planet finished: a rising fanfare that lands an octave up, longer than the run's other chimes. */
    static final byte[] PLANET_FANFARE = chime(new double[]{392.00, 523.25, 659.25, 783.99, 659.25, 1046.5}, 0.13, 0.3);

    /**
     * Four shots at slightly different pitches. Firing the identical sample over
     * and over gets fatiguing fast, so callers pick one of these at random.
     */
    static final byte[][] SHOOT_VARIANTS = {
        tone(1100, 350, 0.12, 0.22),
        tone(1180, 375, 0.115, 0.21),
        tone(1030, 330, 0.125, 0.22),
        tone(1240, 395, 0.11, 0.20)
    };

    private static final Random VARIANT_RNG = new Random();

    static byte[] shootVariant() {
        return SHOOT_VARIANTS[VARIANT_RNG.nextInt(SHOOT_VARIANTS.length)];
    }

    /**
     * A shot for any laser: a pitch sweep from f0 to f1 that fades out, with
     * optional colour. square blends the sine toward a buzzier square wave,
     * overtone adds a bell-like octave, noise adds a rush of air, and vibrato
     * wobbles the pitch. The phase is accumulated, so sweeps stay smooth.
     */
    static byte[] shot(double f0, double f1, double dur, double vol,
                       double square, double overtone, double noise, double vibHz, double vibDepth) {
        int n = (int) (dur * SR);
        byte[] data = new byte[n * 2];
        Random r = new Random(7);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double k = i / (double) n, t = i / (double) SR;
            double freq = (f0 + (f1 - f0) * k) * (1 + vibDepth * Math.sin(2 * Math.PI * vibHz * t));
            phase += 2 * Math.PI * freq / SR;
            double sine = Math.sin(phase);
            double wave = sine * (1 - square) + Math.signum(sine) * 0.6 * square + Math.sin(2 * phase) * overtone;
            double env = Math.pow(1 - k, 1.5);
            writeSample(data, i * 2, (wave * (1 - noise) + (r.nextDouble() * 2 - 1) * noise) * vol * env);
        }
        return data;
    }

    /** How the variants of each laser differ: the same sound a shade lower and higher. */
    static final double[] LASER_PITCHES = { 1.0, 0.94, 1.06 };

    /**
     * Each paint's laser sound, matched to how it looks: the classic bolt keeps
     * the original zap; a fireball whooshes, an iron slug thuds, an ice shard
     * rings like glass, a void ring falls away into nothing, and so on.
     */
    static byte[] laserSound(ShipSkin.LaserShape shape, double p) {
        return switch (shape) {
            case BOLT -> tone(1100 * p, 350 * p, 0.12, 0.22);
            case FIREBALL -> shot(420 * p, 140 * p, 0.18, 0.26, 0.0, 0.0, 0.55, 0, 0);
            case ORB -> shot(700 * p, 520 * p, 0.15, 0.24, 0.0, 0.25, 0.0, 22, 0.04);
            case HELIX -> shot(950 * p, 620 * p, 0.14, 0.21, 0.0, 0.0, 0.0, 38, 0.12);
            case SLUG -> shot(230 * p, 85 * p, 0.14, 0.28, 0.6, 0.0, 0.25, 0, 0);
            case TEARDROP -> shot(650 * p, 1300 * p, 0.09, 0.22, 0.0, 0.15, 0.0, 0, 0);
            case LANCE -> shot(2100 * p, 1550 * p, 0.10, 0.17, 0.0, 0.0, 0.0, 0, 0);
            case SHARD -> shot(2500 * p, 2050 * p, 0.10, 0.16, 0.0, 0.45, 0.0, 0, 0);
            case VOID -> shot(520 * p, 70 * p, 0.20, 0.24, 0.15, 0.0, 0.05, 9, 0.08);
            case STAR -> chime(new double[]{ 1567.98 * p, 2093.0 * p, 2637.0 * p }, 0.04, 0.18);
        };
    }

    /** Every laser's variants, made once at start-up. */
    static final java.util.Map<ShipSkin.LaserShape, byte[][]> LASER_SOUNDS = new java.util.EnumMap<>(ShipSkin.LaserShape.class);
    static {
        for (ShipSkin.LaserShape shape : ShipSkin.LaserShape.values()) {
            byte[][] variants = new byte[LASER_PITCHES.length][];
            for (int i = 0; i < variants.length; i++) variants[i] = laserSound(shape, LASER_PITCHES[i]);
            LASER_SOUNDS.put(shape, variants);
        }
    }

    /** A shot in the given laser's sound, at one of its pitches picked at random. */
    static byte[] laserShot(ShipSkin.LaserShape shape) {
        if (shape == ShipSkin.LaserShape.BOLT) return shootVariant();
        byte[][] variants = LASER_SOUNDS.get(shape);
        return variants[VARIANT_RNG.nextInt(variants.length)];
    }

    /** One distinct sound per power-up, so you know what you grabbed without looking. */
    static final byte[] PICKUP_SHIELD = chime(new double[]{392.00, 587.33, 783.99}, 0.075, 0.26);
    static final byte[] PICKUP_FREEZE = chime(new double[]{1318.5, 1567.98, 2093.0}, 0.055, 0.20);
    static final byte[] PICKUP_FIFTY = chime(new double[]{659.25, 554.37, 880.00}, 0.07, 0.24);

    static byte[] pickupSound(PowerType type) {
        return switch (type) {
            case SHIELD -> PICKUP_SHIELD;
            case FREEZE -> PICKUP_FREEZE;
            default -> PICKUP_FIFTY;
        };
    }

    // ------------------------------------------------------------- loudness

    /**
     * Playback gains, 0 for silence and 1 for the track as recorded. These are
     * the master already folded in, so a line only has to apply the one number.
     */
    private static volatile double musicGain = 1.0, effectsGain = 1.0;

    /**
     * Fixed trim on every music track, under the settings slider: the tracks
     * sat too loud against the effects. A quarter left the menu's soft intro
     * close to inaudible, so 100% in settings now plays at 0.4.
     */
    private static final double MUSIC_TRIM = 0.4;

    /**
     * Sets a line's volume from a 0..1 level.
     *
     * MASTER_GAIN is measured in decibels, which is logarithmic and has no
     * value for true silence, so 0 is sent to the control's own minimum rather
     * than to the negative infinity the conversion would otherwise produce.
     * Lines that offer no gain control are left alone.
     */
    private static void applyGain(Line line, double level) {
        try {
            if (line == null || !line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
            FloatControl c = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = level <= 0.0001
                ? c.getMinimum()
                : (float) Math.max(c.getMinimum(), Math.min(c.getMaximum(), 20 * Math.log10(level)));
            c.setValue(dB);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // A line closing under us, or a control that went away: skip the change.
        }
    }

    /**
     * A temporary dip under the music, 1 for none. Kept apart from musicGain so
     * the game can lower the track while something is being read without
     * touching the level the player chose.
     */
    private static volatile double musicDuck = 1.0;

    private static double musicLineLevel() { return musicGain * MUSIC_TRIM * musicDuck; }

    /** Volume for the background track, applied at once and to later clips. */
    static void setMusicGain(double level) {
        musicGain = Math.max(0, Math.min(1, level));
        MusicStream s = music;
        if (s != null) applyGain(s.line, lineLevel(s));
    }

    /**
     * What a track's line plays at: the settings and the dip, times the
     * track's own leveling. Never above 1, so a raised quiet track can only
     * be brought up to its recorded peaks and never pushed into distortion.
     */
    private static double lineLevel(MusicStream s) {
        return Math.min(1.0, musicLineLevel() * s.trackLevel);
    }

    // ------------------------------------------------------------ leveling

    /**
     * The typical loudness every music file is brought to. The supplied
     * tracks were mastered as much as 9 dB apart - the menu theme and the
     * star's theme sit far below the planet music - so under the shared music
     * trim the menu was close to silent while the hard track was full.
     */
    static final double TARGET_TYPICAL_DB = -12.0;

    /** How far leveling may move a track either way, so a near-silent file is not blown up. */
    static final double MAX_LEVELING_DB = 12.0;

    /**
     * How much louder than the shared level the menu's theme plays. It is the
     * first thing heard and has nothing to compete with, so it may stand out.
     */
    static final double MENU_BOOST_DB = 3.0;

    /**
     * The menu's theme opens on a long, soft fade-in, so the first launch
     * skips to where the track first comes within this many dB of its typical
     * loudness. Only the first pass skips; the loop plays the whole track.
     */
    static final double INTRO_SKIP_DB = 6.0;

    /**
     * Where the menu's theme starts when its file cannot be measured, and the
     * furthest a measured start may skip, so an odd file never loses much.
     */
    static final double MENU_FIXED_START_SECONDS = 15.0;

    /** What one read of a file found: its typical loudness and where it first gets loud, NaN when unmeasurable. */
    private record Measure(double typicalDb, double loudStartSeconds) { }

    /** Each file's measure, kept once taken; keyed by path, size and date so an edited file is measured again. */
    private static final Map<String, Measure> MEASURED = new ConcurrentHashMap<>();

    /** Reads the whole file once - a few tens of milliseconds - on the music thread, never the drawing one. */
    private static Measure measure(File f) {
        String key = f.getAbsolutePath() + "|" + f.length() + "|" + f.lastModified();
        return MEASURED.computeIfAbsent(key, k -> {
            double[] seconds = secondLoudnessDb(f);
            double typical = typicalOf(seconds);
            return new Measure(typical, Double.isNaN(typical) ? Double.NaN : loudStartSeconds(seconds, typical));
        });
    }

    /** The factor that brings a music file to the shared loudness, 1 for a file that cannot be measured. */
    static double trackLevel(File f) {
        double typical = measure(f).typicalDb();
        return Double.isNaN(typical) ? 1.0 : levelingFactor(typical);
    }

    /** A track's playback factor: its leveling, plus the menu's extra lift. */
    static double levelFor(int track, File f) {
        return trackLevel(f) * (track == MENU_TRACK ? Math.pow(10, MENU_BOOST_DB / 20) : 1.0);
    }

    /** Where the menu's theme starts on first launch, in seconds. */
    static double menuStartSeconds(File f) {
        double measured = measure(f).loudStartSeconds();
        return Double.isNaN(measured) ? MENU_FIXED_START_SECONDS : Math.min(measured, MENU_FIXED_START_SECONDS);
    }

    /** The first second within INTRO_SKIP_DB of the typical loudness, 0 when none is. */
    static double loudStartSeconds(double[] seconds, double typicalDb) {
        for (int i = 0; i < seconds.length; i++) {
            if (seconds[i] >= typicalDb - INTRO_SKIP_DB) return i;
        }
        return 0;
    }

    static double levelingFactor(double typicalDb) {
        double gainDb = Math.max(-MAX_LEVELING_DB, Math.min(MAX_LEVELING_DB, TARGET_TYPICAL_DB - typicalDb));
        return Math.pow(10, gainDb / 20);
    }

    /**
     * How loud a file typically is: the median of its one-second loudness
     * readings, leaving out silence. A median rather than an average, so a
     * quiet intro or a loud finale does not decide the level of the whole
     * track. NaN when the file is not 16-bit PCM or cannot be read.
     */
    static double typicalLoudnessDb(File f) {
        return typicalOf(secondLoudnessDb(f));
    }

    /** The median of the readings louder than silence, NaN for none. */
    private static double typicalOf(double[] seconds) {
        if (seconds == null) return Double.NaN;
        List<Double> heard = new ArrayList<>();
        for (double db : seconds) if (db > -50) heard.add(db);
        if (heard.isEmpty()) return Double.NaN;
        java.util.Collections.sort(heard);
        return heard.get(heard.size() / 2);
    }

    /** One loudness reading per second of the file, in order; null when it is not 16-bit PCM or cannot be read. */
    static double[] secondLoudnessDb(File f) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
            AudioFormat fmt = in.getFormat();
            if (fmt.getSampleSizeInBits() != 16 || !AudioFormat.Encoding.PCM_SIGNED.equals(fmt.getEncoding())) return null;
            boolean big = fmt.isBigEndian();
            int bytesPerSecond = (int) fmt.getSampleRate() * fmt.getFrameSize();
            byte[] buf = new byte[Math.max(2, bytesPerSecond - bytesPerSecond % 2)];
            List<Double> seconds = new ArrayList<>();
            int read;
            while ((read = in.readNBytes(buf, 0, buf.length)) > 1) {
                double sumSq = 0;
                int samples = read / 2;
                for (int i = 0; i + 1 < read; i += 2) {
                    int v = big ? (short) ((buf[i] << 8) | (buf[i + 1] & 0xFF)) : (short) ((buf[i] & 0xFF) | (buf[i + 1] << 8));
                    sumSq += (double) v * v;
                }
                seconds.add(20 * Math.log10(Math.max(Math.sqrt(sumSq / samples) / 32768.0, 1e-9)));
            }
            return seconds.stream().mapToDouble(Double::doubleValue).toArray();
        } catch (IOException | UnsupportedAudioFileException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Sets the dip, 0..1. Cheap to call every frame: an unchanged value is skipped. */
    static void setMusicDuck(double level) {
        level = Math.max(0, Math.min(1, level));
        if (Math.abs(level - musicDuck) < 0.002) return;
        musicDuck = level;
        MusicStream s = music;
        if (s != null) applyGain(s.line, lineLevel(s));
    }

    /** Volume for the sound effects, picked up by every sound played after it. */
    static void setEffectsGain(double level) {
        effectsGain = Math.max(0, Math.min(1, level));
    }

    static void play(byte[] pcm) {
        double level = effectsGain;
        if (level <= 0.0001) return;          // nothing to hear - skip the line entirely
        PLAYERS.execute(() -> {
            try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
                line.open(FORMAT);
                applyGain(line, level);
                line.start();
                line.write(pcm, 0, pcm.length);
                line.drain();
            } catch (LineUnavailableException | IllegalArgumentException | IllegalStateException | SecurityException ignored) {
                // No free output line right now: this one sound is skipped.
            }
        });
    }

    // ---------------------------------------------------------------- music

    /** Root note per difficulty sector, so each stretch of the journey has its own key. */
    private static final double[] SECTOR_ROOT = { 196.00, 174.61, 146.83 };

    /**
     * The file each difficulty sector plays instead of its generated pad, in
     * the order easy, medium, hard. A name with no file behind it falls back to
     * the pad, so the sectors can be filled in one at a time.
     */
    private static final String[] SECTOR_FILE = { "easy_music.wav", "medium_music.wav", "hard_music.wav" };
    private static final int[][] PROGRESSION = { {0, 4, 7}, {-3, 2, 5}, {2, 5, 9}, {-5, 0, 4} };

    /** Passed to startMusic in place of a sector to request the star's theme. */
    static final int BOSS_TRACK = -99;

    /**
     * Passed to startMusic in place of a sector to request the menu's theme.
     *
     * The menu used to share sector 0's track with the easy planets. It needs
     * its own so a supplied menu_music.wav stops where the game starts, rather
     * than playing on through the first stretch of the journey.
     */
    static final int MENU_TRACK = -98;

    /** The track playing now, or null for silence. Only the game's own thread replaces it. */
    private static volatile MusicStream music;

    /** Generated loops are kept once built - they take a moment to synthesize. */
    private static final Map<Integer, byte[]> LOOP_CACHE = new ConcurrentHashMap<>();

    /**
     * Music files that are missing or would not play, one line each, for the
     * main menu to show. Without it a bad or misplaced file just left a track on
     * its generated loop, or silent, with nothing to say why.
     */
    private static final List<String> MUSIC_PROBLEMS = new CopyOnWriteArrayList<>();

    /**
     * Builds a slow four-chord pad loop from pure sine tones. Each chord fades
     * in and out so the loop has no clicks at the seams.
     */
    static byte[] musicLoop(int sector) {
        double root = SECTOR_ROOT[Math.max(0, Math.min(SECTOR_ROOT.length - 1, sector))];
        double chordDur = 2.6;
        int nPerChord = (int) (chordDur * SR);
        byte[] data = new byte[nPerChord * 2 * PROGRESSION.length];
        int off = 0;

        for (int[] chord : PROGRESSION) {
            for (int i = 0; i < nPerChord; i++) {
                double t = i / (double) SR;
                double p = i / (double) nPerChord;
                double env = Math.sin(p * Math.PI);
                double sample = 0;
                for (int semitone : chord) {
                    double f = root * Math.pow(2, semitone / 12.0);
                    sample += Math.sin(2 * Math.PI * f * t) * 0.30;
                    sample += Math.sin(2 * Math.PI * f * 2 * t) * 0.07;
                }
                writeSample(data, off, sample * env * 0.30);
                off += 2;
            }
        }
        return data;
    }

    /**
     * The star's theme: a driving minor loop for the final level.
     *
     * Four chords over eight seconds, with a punchy bass note on every eighth,
     * a sustained pad underneath and an arpeggio riding on top. Every part uses
     * an envelope that starts and ends at silence, so the loop seams are clean.
     */
    static byte[] bossLoop() {
        double root = 110.0;                 // A2
        double beat = 0.125;                 // sixteenths at 240 BPM - twice the old pace
        int beatsPerChord = 16;
        // A darker turn than the planet music: minor, then a flattened sixth and
        // second that refuse to settle.
        int[][] chords = { {0, 3, 7}, {-4, 0, 3}, {1, 5, 8}, {-2, 2, 5} };
        int[] bassNote = { 0, -4, 1, -2 };
        int[] arpStep = { 0, 2, 1, 2, 0, 1, 2, 1 };

        int nPerBeat = (int) (beat * SR);
        int nPerChord = nPerBeat * beatsPerChord;
        byte[] data = new byte[nPerChord * 2 * chords.length];
        int off = 0;
        Random noise = new Random(90210);    // fixed, so the loop is reproducible

        for (int c = 0; c < chords.length; c++) {
            int[] chord = chords[c];
            double bassF = root * Math.pow(2, bassNote[c] / 12.0);

            for (int i = 0; i < nPerChord; i++) {
                double t = i / (double) SR;
                double chordP = i / (double) nPerChord;
                int beatIndex = i / nPerBeat;
                double beatT = (i % nPerBeat) / (double) SR;
                double beatP = (i % nPerBeat) / (double) nPerBeat;
                double hit = noise.nextDouble() * 2 - 1;

                double sample = 0;

                // Bass: a driving sawtooth rather than a sine. The extra harmonics
                // are what make it sound aggressive instead of smooth.
                double bassEnv = Math.exp(-beatP * 3.4) * (1 - beatP);
                for (int h = 1; h <= 5; h++) {
                    sample += Math.sin(2 * Math.PI * bassF * h * t) * (0.50 / h) * bassEnv;
                }

                // Pad: the chord an octave up, slightly detuned against itself so
                // it beats and grinds rather than sitting still.
                double padEnv = Math.sin(chordP * Math.PI) * 0.30;
                for (int semi : chord) {
                    double f = root * 2 * Math.pow(2, semi / 12.0);
                    sample += Math.sin(2 * Math.PI * f * t) * 0.13 * padEnv;
                    sample += Math.sin(2 * Math.PI * f * 1.006 * t) * 0.09 * padEnv;
                }

                // Arpeggio: sixteenth notes, two octaves up.
                double arpF = root * 4 * Math.pow(2, chord[arpStep[beatIndex % arpStep.length]] / 12.0);
                double arpEnv = Math.exp(-beatP * 7.0) * (1 - beatP);
                sample += Math.sin(2 * Math.PI * arpF * beatT) * 0.15 * arpEnv;

                // Kick on the beat.
                if (beatIndex % 4 == 0) {
                    double kickEnv = Math.exp(-beatP * 9.0);
                    double sweep = 96 - 46 * beatP;      // pitch drops as it hits
                    sample += Math.sin(2 * Math.PI * sweep * beatT) * 0.44 * kickEnv;
                }

                // Snare on the backbeat: filtered noise plus a little body.
                if (beatIndex % 8 == 4) {
                    double snareEnv = Math.exp(-beatP * 13.0);
                    sample += hit * 0.34 * snareEnv;
                    sample += Math.sin(2 * Math.PI * 190 * beatT) * 0.18 * snareEnv;
                }

                // Hats ticking through every offbeat sixteenth.
                if (beatIndex % 2 == 1) {
                    double hatEnv = Math.exp(-beatP * 30.0);
                    sample += hit * 0.10 * hatEnv;
                }

                writeSample(data, off, sample * 0.52);
                off += 2;
            }
        }
        return data;
    }

    private static byte[] loopFor(int sector) {
        return LOOP_CACHE.computeIfAbsent(sector,
            s -> s == BOSS_TRACK ? bossLoop() : musicLoop(s));
    }

    /** The file that replaces a track's generated loop, or null for a track that has none. */
    static String fileFor(int track) {
        return track == BOSS_TRACK ? "boss_music.wav"
            : track == MENU_TRACK ? "menu_music.wav"
            : track >= 0 && track < SECTOR_FILE.length ? SECTOR_FILE[track]
            : null;
    }

    /** Every music file the game looks for, menu first. */
    static String[] musicFiles() {
        return new String[]{ fileFor(MENU_TRACK), SECTOR_FILE[0], SECTOR_FILE[1], SECTOR_FILE[2], fileFor(BOSS_TRACK) };
    }

    /**
     * Looks over every music file once at start-up: missing, unreadable, or in
     * a format this computer cannot play. Only the file headers are read, so it
     * costs next to nothing. What it finds is shown on the main menu.
     */
    /**
     * Brings up the audio system and builds every generated sound (touching
     * this class does that), so the first sound or menu track does not pay
     * for it. Safe from any thread; see RocketLearn.warmUpInBackground.
     */
    static void warmUp() {
        try {
            AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, FORMAT));
        } catch (RuntimeException ignored) {
            // No sound system at all: checkMusicFiles reports it later.
        }
    }

    static void checkMusicFiles() { checkMusicFiles(GameFiles.dir()); }

    static void checkMusicFiles(File dir) {
        MUSIC_PROBLEMS.clear();
        // With no sound output at all every file would look unplayable, which
        // would blame the files for the computer. Say what is really wrong.
        boolean anyOutput = AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, FORMAT));
        if (!anyOutput) reportMusicProblem("no sound output is available on this computer");
        List<String> missing = new ArrayList<>();
        for (String name : musicFiles()) {
            File f = new File(dir, name);
            if (!f.isFile()) { missing.add(name); continue; }
            try {
                AudioFormat fmt = AudioSystem.getAudioFileFormat(f).getFormat();
                if (anyOutput && playableFormat(fmt) == null) reportMusicProblem(name + " is in a format this computer cannot play");
            } catch (UnsupportedAudioFileException e) {
                reportMusicProblem(name + " is not a WAV file Java can read");
            } catch (IOException e) {
                reportMusicProblem(name + " could not be read");
            }
        }
        if (missing.size() == musicFiles().length) {
            reportMusicProblem("no music files found in " + dir.getAbsoluteFile().toPath().normalize());
        } else if (!missing.isEmpty()) {
            reportMusicProblem(String.join(", ", missing) + (missing.size() == 1 ? " is" : " are") + " missing");
        }
    }

    private static void reportMusicProblem(String problem) {
        if (!MUSIC_PROBLEMS.contains(problem)) MUSIC_PROBLEMS.add(problem);
        System.err.println("RocketLearn music: " + problem);
    }

    /** One line for the main menu about the music, or null when every track is fine. */
    static String musicStatus() {
        if (MUSIC_PROBLEMS.isEmpty()) return null;
        String first = MUSIC_PROBLEMS.get(0);
        String more = MUSIC_PROBLEMS.size() > 1 ? "  (+" + (MUSIC_PROBLEMS.size() - 1) + " more)" : "";
        return "Music: " + first + more + " - built-in music plays instead";
    }

    static List<String> musicProblems() { return MUSIC_PROBLEMS; }

    /**
     * The format to play a file in: its own when a speaker line takes it,
     * otherwise plain 16-bit PCM at the same rate, or null when neither works.
     */
    static AudioFormat playableFormat(AudioFormat fmt) {
        if (AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, fmt))) return fmt;
        AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fmt.getSampleRate(), 16,
            fmt.getChannels(), fmt.getChannels() * 2, fmt.getSampleRate(), false);
        boolean convertible = AudioSystem.isConversionSupported(pcm, fmt);
        return convertible && AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, pcm)) ? pcm : null;
    }

    /**
     * One track, played by streaming it a small piece at a time on its own
     * thread.
     *
     * The music used to be loaded whole into a Clip on the game's drawing
     * thread each time the track changed. The easy track alone is 38 MB, so
     * crossing into a new stretch of the journey froze the game while it
     * loaded, which a slow machine could not hide. A stream starts at once,
     * holds a fraction of a second in memory, and leaves the drawing alone.
     */
    private static final class MusicStream implements Runnable {
        final int track;
        volatile boolean stopped;
        volatile SourceDataLine line;
        /** This track's leveling factor: measured for a file, 1 for a generated loop. */
        volatile double trackLevel = 1.0;

        MusicStream(int track) { this.track = track; }

        @Override
        public void run() {
            String name = fileFor(track);
            File file = name == null ? null : GameFiles.file(name);
            boolean fromFile = file != null && file.isFile();
            // Measured here, on the music thread, before the first note plays,
            // so the level never jumps once the track is under way.
            if (fromFile) trackLevel = levelFor(track, file);
            // The menu's first pass skips its soft intro; every loop after plays it all.
            double skip = fromFile && track == MENU_TRACK ? menuStartSeconds(file) : 0;
            try {
                while (!stopped) {
                    try (AudioInputStream in = fromFile ? openFile(file, skip) : openLoop()) {
                        skip = 0;
                        play(in);
                    } catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                             | IllegalArgumentException | IllegalStateException | SecurityException e) {
                        if (stopped) return;
                        if (!fromFile) {
                            // Even the generated loop cannot play: there is no
                            // usable sound output. Say so once and go quiet.
                            reportMusicProblem("no sound output is available (" + e.getMessage() + ")");
                            return;
                        }
                        // A file that fails falls back to the built-in loop, and says so.
                        reportMusicProblem(name + " could not be played (" + e.getMessage() + ")");
                        fromFile = false;
                        trackLevel = 1.0;
                        closeLine();
                    }
                }
            } finally {
                closeLine();
            }
        }

        private AudioInputStream openFile(File f, double skipSeconds) throws IOException, UnsupportedAudioFileException {
            // The format is checked from the header before the stream is opened,
            // so a file that cannot play never leaves a stream to clean up.
            AudioFormat own = AudioSystem.getAudioFileFormat(f).getFormat();
            AudioFormat fmt = playableFormat(own);
            if (fmt == null) throw new UnsupportedAudioFileException("format " + own);
            AudioInputStream in = AudioSystem.getAudioInputStream(f);
            AudioInputStream out = fmt.equals(in.getFormat()) ? in : AudioSystem.getAudioInputStream(fmt, in);
            if (skipSeconds > 0) {
                try {
                    long bytes = (long) (skipSeconds * fmt.getFrameRate()) * fmt.getFrameSize();
                    long k;
                    while (bytes > 0 && (k = out.skip(bytes)) > 0) bytes -= k;
                } catch (IOException e) {
                    out.close();
                    throw e;
                }
            }
            return out;
        }

        private AudioInputStream openLoop() {
            byte[] pcm = loopFor(track);
            return new AudioInputStream(new ByteArrayInputStream(pcm), FORMAT, pcm.length / FORMAT.getFrameSize());
        }

        /** Feeds one pass of the stream to the speaker; the caller loops it. */
        private void play(AudioInputStream in) throws LineUnavailableException, IOException {
            AudioFormat fmt = in.getFormat();
            SourceDataLine l = line;
            if (l == null || !l.getFormat().matches(fmt)) {
                closeLine();
                l = AudioSystem.getSourceDataLine(fmt);
                // About a third of a second of buffer: short enough that a
                // pause or a track change is heard at once.
                int frames = (int) (fmt.getFrameRate() / 3);
                l.open(fmt, Math.max(fmt.getFrameSize() * frames, fmt.getFrameSize() * 1024));
                line = l;
                applyGain(l, lineLevel(this));
                l.start();
            }
            int frameSize = Math.max(1, fmt.getFrameSize());
            byte[] buf = new byte[frameSize * 4096];
            int n;
            // write() blocks while the line is paused, so a paused track sits
            // here without spinning and carries on from the same spot.
            while (!stopped && (n = in.read(buf)) > 0) {
                l.write(buf, 0, n - n % frameSize);
            }
        }

        void pause() {
            SourceDataLine l = line;
            if (l != null) l.stop();
        }

        void resume() {
            SourceDataLine l = line;
            if (l != null && !l.isRunning()) l.start();
        }

        void stop() {
            stopped = true;
            closeLine();
        }

        private void closeLine() {
            SourceDataLine l = line;
            line = null;
            if (l == null) return;
            try {
                l.stop();
                l.flush();
                l.close();
            } catch (RuntimeException ignored) {
                // Already closed by the other side of a stop.
            }
        }
    }

    /**
     * Starts (or switches to) the loop for a track. Safe to call repeatedly.
     *
     * Asking again for the track already playing picks it up where it is, so
     * coming back from a pause carries on mid-phrase instead of snapping to
     * the top. Returns at once: the track starts on its own thread.
     */
    static void startMusic(int track) {
        MusicStream s = music;
        if (s != null && s.track == track && !s.stopped) {
            s.resume();
            return;
        }
        stopMusic();
        MusicStream next = new MusicStream(track);
        music = next;
        Thread t = new Thread(next, "music");
        t.setDaemon(true);
        t.start();
    }

    /** Holds the track where it is; startMusic with the same track resumes it from the same spot. */
    static void pauseMusic() {
        MusicStream s = music;
        if (s != null) s.pause();
    }

    static void stopMusic() {
        MusicStream s = music;
        music = null;
        if (s == null) return;
        s.stopped = true;
        // Closing a speaker line can take a tenth of a second on Windows, and
        // this is called from the drawing thread, so the close happens beside it.
        Thread closer = new Thread(s::stop, "music-stop");
        closer.setDaemon(true);
        closer.start();
    }

    /** The track playing now, or Integer.MIN_VALUE for none. */
    static int currentTrack() {
        MusicStream s = music;
        return s == null ? Integer.MIN_VALUE : s.track;
    }

    private Sfx() { }
}
