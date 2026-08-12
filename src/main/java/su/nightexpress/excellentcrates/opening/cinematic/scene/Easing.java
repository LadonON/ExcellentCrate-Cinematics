package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.Enums;

import java.util.function.DoubleUnaryOperator;

/**
 * Interpolation curves for timed camera and prop movement.
 *
 * <p>Each curve maps linear progress through a frame ({@code 0.0} at the first tick, {@code 1.0} at
 * the last) onto eased progress. A camera that starts and stops abruptly reads as a glitch, so
 * {@link #EASE_IN_OUT} is the default for movement frames.
 */
public enum Easing {

    /** Constant speed. */
    LINEAR("Linear", t -> t),

    /** Starts slow, accelerates. Good for pulling away from a subject. */
    EASE_IN("Ease In", t -> t * t),

    /** Starts fast, decelerates. Good for settling onto a subject. */
    EASE_OUT("Ease Out", t -> t * (2.0D - t)),

    /** Slow at both ends, fast through the middle. The most natural-looking camera move. */
    EASE_IN_OUT("Ease In / Out", t -> t < 0.5D ? 2.0D * t * t : -1.0D + (4.0D - 2.0D * t) * t);

    private final String              name;
    private final DoubleUnaryOperator curve;

    Easing(@NotNull String name, @NotNull DoubleUnaryOperator curve) {
        this.name = name;
        this.curve = curve;
    }

    /**
     * Resolves an easing by name, falling back to {@link #EASE_IN_OUT} for unknown or hand-edited
     * values so a typo in a config never aborts scene loading.
     */
    @NotNull
    public static Easing byName(@NotNull String name) {
        return Enums.parse(name, Easing.class).orElse(EASE_IN_OUT);
    }

    /**
     * Applies the curve.
     *
     * @param progress linear progress, clamped into {@code [0, 1]} before the curve is applied so
     *                 rounding at the frame boundaries cannot overshoot the target.
     * @return eased progress in {@code [0, 1]}.
     */
    public double apply(double progress) {
        double clamped = Math.clamp(progress, 0.0D, 1.0D);
        return this.curve.applyAsDouble(clamped);
    }

    @NotNull
    public String getName() {
        return this.name;
    }
}
