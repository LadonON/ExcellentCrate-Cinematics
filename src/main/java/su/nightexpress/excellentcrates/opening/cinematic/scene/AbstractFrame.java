package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Shared duration handling for every frame implementation.
 */
public abstract class AbstractFrame implements CinematicFrame {

    /** Config key holding the {@link FrameType} discriminator. */
    public static final String KEY_TYPE = "Type";

    /** Config key holding the frame length in ticks. */
    public static final String KEY_DURATION = "Duration";

    protected long duration;

    protected AbstractFrame(long duration) {
        this.setDuration(duration);
    }

    @Override
    public long getDuration() {
        return this.duration;
    }

    /**
     * Negative durations would make the timeline run backwards, so they are clamped to zero.
     */
    @Override
    public void setDuration(long duration) {
        this.duration = Math.max(0L, duration);
    }

    /**
     * Writes the fields common to every frame. Implementations call this first, then add their own.
     */
    protected void writeBase(@NotNull ConfigurationSection config, @NotNull String path) {
        config.set(path + "." + KEY_TYPE, this.getType().name());
        config.set(path + "." + KEY_DURATION, this.duration);
    }

    /**
     * Reads the frame length, falling back to the type's default when the key is absent.
     */
    protected static long readDuration(@NotNull ConfigurationSection config, @NotNull String path, long fallback) {
        return config.getLong(path + "." + KEY_DURATION, fallback);
    }
}
