package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.Easing;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;

/**
 * Glides the camera from wherever it currently is to {@link #getTarget()} across the frame's
 * duration, following an {@link Easing} curve.
 *
 * <p>The start point is deliberately not stored: it is whatever the previous frame left behind, so
 * inserting or reordering frames cannot leave a stale, mismatched start position behind.
 */
public class MoveCameraFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 40L;

    public static final String KEY_TARGET = "Target";
    public static final String KEY_EASING = "Easing";

    private WorldPoint target;
    private Easing     easing;

    public MoveCameraFrame(long duration, @NotNull WorldPoint target, @NotNull Easing easing) {
        super(duration);
        this.target = target;
        this.easing = easing;
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new MoveCameraFrame(DEFAULT_DURATION, WorldPoint.empty(), Easing.EASE_IN_OUT);
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        long duration = readDuration(config, path, DEFAULT_DURATION);
        WorldPoint target = WorldPoint.read(config, path + "." + KEY_TARGET);
        Easing easing = Easing.byName(config.getString(path + "." + KEY_EASING, ""));

        return new MoveCameraFrame(duration, target, easing);
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.MOVE_CAMERA;
    }

    @Override
    @NotNull
    public String getSummary() {
        return "Move Camera " + CinematicFrame.formatTicks(this.duration);
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
        this.target.write(config, path + "." + KEY_TARGET);
        config.set(path + "." + KEY_EASING, this.easing.name());
    }

    @NotNull
    public WorldPoint getTarget() {
        return this.target;
    }

    public void setTarget(@NotNull WorldPoint target) {
        this.target = target;
    }

    @NotNull
    public Easing getEasing() {
        return this.easing;
    }

    public void setEasing(@NotNull Easing easing) {
        this.easing = easing;
    }
}
