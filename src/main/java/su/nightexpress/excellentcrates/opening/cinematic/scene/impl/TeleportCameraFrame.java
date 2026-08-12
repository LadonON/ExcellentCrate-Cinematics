package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;

/**
 * Cuts the camera straight to a target with no interpolation — the cinematic equivalent of a shot
 * change.
 *
 * <p>Defaults to zero duration so the next frame begins in the same tick.
 */
public class TeleportCameraFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 0L;

    public static final String KEY_TARGET = "Target";

    private WorldPoint target;

    public TeleportCameraFrame(long duration, @NotNull WorldPoint target) {
        super(duration);
        this.target = target;
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new TeleportCameraFrame(DEFAULT_DURATION, WorldPoint.empty());
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        return new TeleportCameraFrame(readDuration(config, path, DEFAULT_DURATION), WorldPoint.read(config, path + "." + KEY_TARGET));
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.TELEPORT_CAMERA;
    }

    @Override
    @NotNull
    public String getSummary() {
        return this.target.isEmpty() ? "Teleport Camera (unset)" : "Teleport Camera";
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
        this.target.write(config, path + "." + KEY_TARGET);
    }

    @NotNull
    public WorldPoint getTarget() {
        return this.target;
    }

    public void setTarget(@NotNull WorldPoint target) {
        this.target = target;
    }
}
