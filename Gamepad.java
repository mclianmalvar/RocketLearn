package rocketlearn;

import java.awt.GraphicsEnvironment;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Reads an Xbox-style controller through Windows' XInput, using Java's
 * foreign function API so the game still needs no library beside it.
 *
 * Anywhere XInput cannot be reached - another OS, a headless test run, or a
 * JVM that refuses native access - the controller is simply reported as
 * unavailable and poll() always answers "not connected".
 *
 * Launch with --enable-native-access=ALL-UNNAMED to keep the JVM from
 * printing a warning the first time the DLL is loaded.
 */
final class Gamepad {

    static final int DPAD_UP = 0x0001, DPAD_DOWN = 0x0002, DPAD_LEFT = 0x0004, DPAD_RIGHT = 0x0008;
    static final int START = 0x0010, BACK = 0x0020;
    static final int LB = 0x0100, RB = 0x0200;
    static final int A = 0x1000, B = 0x2000, X = 0x4000, Y = 0x8000;

    /** One reading of the pad. Sticks run -1..1 with up positive; triggers 0..1. */
    static final class Snapshot {
        final boolean connected;
        final int buttons;
        final double lx, ly, lt, rt;

        Snapshot(boolean connected, int buttons, double lx, double ly, double lt, double rt) {
            this.connected = connected;
            this.buttons = buttons;
            this.lx = lx;
            this.ly = ly;
            this.lt = lt;
            this.rt = rt;
        }

        boolean has(int button) { return (buttons & button) != 0; }
    }

    static final Snapshot NONE = new Snapshot(false, 0, 0, 0, 0, 0);

    /** XInputGetState(DWORD dwUserIndex, XINPUT_STATE* pState) -> DWORD, or null when unreachable. */
    private static final MethodHandle GET_STATE;
    /** XINPUT_STATE: packet number, then XINPUT_GAMEPAD - 16 bytes in all. */
    private static final MemorySegment STATE;

    static {
        MethodHandle handle = null;
        MemorySegment state = null;
        try {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
            if (windows && !GraphicsEnvironment.isHeadless()) {
                SymbolLookup lib = null;
                // 1_4 ships with Windows 8 and later; the others cover older systems.
                for (String dll : new String[]{ "xinput1_4.dll", "xinput1_3.dll", "xinput9_1_0.dll" }) {
                    try {
                        lib = SymbolLookup.libraryLookup(dll, Arena.global());
                        break;
                    } catch (IllegalArgumentException notThere) { }
                }
                if (lib != null) {
                    handle = Linker.nativeLinker().downcallHandle(
                        lib.find("XInputGetState").orElseThrow(),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                    state = Arena.global().allocate(16, 4);
                }
            }
        } catch (Throwable unreachable) {
            handle = null;
        }
        GET_STATE = handle;
        STATE = state;
    }

    /** Which of the four XInput slots the pad was last found in, or -1. */
    private static int slot = -1;
    /**
     * Asking an empty slot is slow on some systems, so with no pad connected
     * the slots are only searched about once a second.
     */
    private static long nextSearchNanos = 0;
    private static volatile boolean connected = false;

    static boolean available() { return GET_STATE != null; }

    static boolean connected() { return connected; }

    static Snapshot poll() {
        if (GET_STATE == null) return NONE;
        try {
            if (slot >= 0) {
                Snapshot s = read(slot);
                if (s != null) return s;
                slot = -1;
            }
            long now = System.nanoTime();
            if (now >= nextSearchNanos) {
                nextSearchNanos = now + 1_000_000_000L;
                for (int i = 0; i < 4; i++) {
                    Snapshot s = read(i);
                    if (s != null) {
                        slot = i;
                        return s;
                    }
                }
            }
        } catch (Throwable ignored) { }
        connected = false;
        return NONE;
    }

    private static Snapshot read(int index) throws Throwable {
        int result = (int) GET_STATE.invokeExact(index, STATE);
        if (result != 0) return null;   // ERROR_DEVICE_NOT_CONNECTED
        int buttons = Short.toUnsignedInt(STATE.get(ValueLayout.JAVA_SHORT, 4));
        int lt = Byte.toUnsignedInt(STATE.get(ValueLayout.JAVA_BYTE, 6));
        int rt = Byte.toUnsignedInt(STATE.get(ValueLayout.JAVA_BYTE, 7));
        short lx = STATE.get(ValueLayout.JAVA_SHORT, 8);
        short ly = STATE.get(ValueLayout.JAVA_SHORT, 10);
        connected = true;
        return new Snapshot(true, buttons, stick(lx), stick(ly), trigger(lt), trigger(rt));
    }

    /** Microsoft's recommended left-stick dead zone, as a fraction of full tilt. */
    static final double STICK_DEADZONE = 7849 / 32767.0;
    static final int TRIGGER_THRESHOLD = 30;

    /** Raw stick axis to -1..1, flat inside the dead zone and rescaled beyond it. */
    static double stick(short raw) {
        double v = Math.max(-1, raw / 32767.0);
        double mag = Math.abs(v);
        if (mag < STICK_DEADZONE) return 0;
        return Math.signum(v) * Math.min(1, (mag - STICK_DEADZONE) / (1 - STICK_DEADZONE));
    }

    /** Raw trigger to 0..1, ignoring the first few counts of a resting trigger. */
    static double trigger(int raw) {
        if (raw < TRIGGER_THRESHOLD) return 0;
        return Math.min(1, (raw - TRIGGER_THRESHOLD) / (255.0 - TRIGGER_THRESHOLD));
    }

    private Gamepad() { }
}
