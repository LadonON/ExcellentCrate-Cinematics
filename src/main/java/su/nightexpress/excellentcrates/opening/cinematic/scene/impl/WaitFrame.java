package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;

/**
 * Holds the timeline still for a while. Nothing happens; the camera and prop stay where the
 * previous frames left them.
 *
 * <p>This is the only way to express a gap, which keeps frame order and timeline order identical.
 */
public class WaitFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 20L;

    public WaitFrame(long duration) {
        super(duration);
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new WaitFrame(DEFAULT_DURATION);
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        return new WaitFrame(readDuration(config, path, DEFAULT_DURATION));
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.WAIT;
    }

    @Override
    @NotNull
    public String getSummary() {
        return "Wait " + CinematicFrame.formatTicks(this.duration);
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
    }
}
