package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Properties every easing curve has to hold, since a curve that misses its endpoints leaves the
 * camera or prop parked slightly off the position the admin captured.
 */
class EasingTest {

    private static final double TOLERANCE = 1.0E-9D;

    @ParameterizedTest
    @EnumSource(Easing.class)
    void curveStartsAtZeroAndEndsAtOne(Easing easing) {
        assertEquals(0.0D, easing.apply(0.0D), TOLERANCE, easing + " does not start at 0");
        assertEquals(1.0D, easing.apply(1.0D), TOLERANCE, easing + " does not end at 1");
    }

    /** Progress can only ever move forwards, so no curve may dip backwards mid-move. */
    @ParameterizedTest
    @EnumSource(Easing.class)
    void curveIsMonotonic(Easing easing) {
        double previous = easing.apply(0.0D);

        for (int step = 1; step <= 100; step++) {
            double current = easing.apply(step / 100.0D);
            assertTrue(current >= previous - TOLERANCE, easing + " went backwards at step " + step);
            previous = current;
        }
    }

    /** Rounding at frame boundaries can hand in slightly out-of-range progress. */
    @ParameterizedTest
    @EnumSource(Easing.class)
    void inputIsClamped(Easing easing) {
        assertEquals(0.0D, easing.apply(-5.0D), TOLERANCE, easing + " overshot below 0");
        assertEquals(1.0D, easing.apply(5.0D), TOLERANCE, easing + " overshot above 1");
    }

    @Test
    void linearIsTheIdentity() {
        assertEquals(0.25D, Easing.LINEAR.apply(0.25D), TOLERANCE);
        assertEquals(0.5D, Easing.LINEAR.apply(0.5D), TOLERANCE);
    }

    /** Ease-in lags behind linear early on; ease-out runs ahead of it. */
    @Test
    void easeInLagsAndEaseOutLeads() {
        double linear = Easing.LINEAR.apply(0.25D);

        assertTrue(Easing.EASE_IN.apply(0.25D) < linear, "EASE_IN should start slower than linear");
        assertTrue(Easing.EASE_OUT.apply(0.25D) > linear, "EASE_OUT should start faster than linear");
    }

    @Test
    void easeInOutIsSymmetricAboutTheMidpoint() {
        assertEquals(0.5D, Easing.EASE_IN_OUT.apply(0.5D), TOLERANCE);

        // f(t) + f(1 - t) == 1 for a curve that is symmetric around its midpoint.
        for (double t = 0.0D; t <= 1.0D; t += 0.1D) {
            assertEquals(1.0D, Easing.EASE_IN_OUT.apply(t) + Easing.EASE_IN_OUT.apply(1.0D - t), 1.0E-9D);
        }
    }

    @Test
    void unknownNameFallsBackToEaseInOut() {
        assertEquals(Easing.EASE_IN_OUT, Easing.byName("not_a_curve"));
        assertEquals(Easing.EASE_IN_OUT, Easing.byName(""));
    }

    @Test
    void knownNameIsParsedCaseInsensitively() {
        assertEquals(Easing.EASE_OUT, Easing.byName("EASE_OUT"));
        assertEquals(Easing.EASE_OUT, Easing.byName("ease_out"));
    }
}
