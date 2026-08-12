package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * One step of a cinematic timeline.
 *
 * <p>Frames follow the same "type + typed options" shape as {@code Cost} and {@code CrateEffect}
 * elsewhere in the plugin: each implementation owns only the fields it actually needs, instead of a
 * single flat map covering every possible option.
 *
 * <p>There is no absolute "time" field — a frame's position in {@link CinematicScene#getFrames()}
 * is its position in the timeline, and gaps are expressed with a
 * {@link su.nightexpress.excellentcrates.opening.cinematic.scene.impl.WaitFrame}. That keeps reading
 * the YAML top-to-bottom identical to reading the editor screen top-to-bottom, which matters
 * because admins hand-edit these files.
 */
public interface CinematicFrame {

    /** Vanilla runs at 20 ticks per second. */
    long TICKS_PER_SECOND = 20L;

    @NotNull
    FrameType getType();

    /**
     * How long this frame occupies the timeline, in ticks.
     *
     * <p>Zero-duration frames (a sound, a title, a cut) fire and hand over to the next frame within
     * the same tick, so several can be stacked to happen simultaneously.
     */
    long getDuration();

    void setDuration(long duration);

    /**
     * A one-line description for the frame's icon in the editor, e.g. {@code "Move Camera → 2.0s"}.
     */
    @NotNull
    String getSummary();

    /**
     * Writes this frame to {@code path}, including the {@code Type} discriminator that
     * {@link FrameType} reads back.
     */
    void write(@NotNull ConfigurationSection config, @NotNull String path);

    /**
     * Formats a tick count as seconds for editor summaries.
     */
    @NotNull
    static String formatTicks(long ticks) {
        return String.format(Locale.US, "%.1fs", (double) ticks / TICKS_PER_SECOND);
    }
}
