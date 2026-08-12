package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;

/**
 * Reveals the reward the crate already rolled, at the scene's reward offset.
 *
 * <p>This frame only presents an outcome — the reward was decided when the opening started, exactly
 * as with the other opening types. The frame's duration is how long the reveal is held on screen
 * before the timeline continues.
 */
public class SpawnRewardFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 60L;

    public static final String KEY_FIREWORK = "Firework";

    private boolean firework;

    public SpawnRewardFrame(long duration, boolean firework) {
        super(duration);
        this.firework = firework;
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new SpawnRewardFrame(DEFAULT_DURATION, true);
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        return new SpawnRewardFrame(readDuration(config, path, DEFAULT_DURATION), config.getBoolean(path + "." + KEY_FIREWORK, true));
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.SPAWN_REWARD;
    }

    @Override
    @NotNull
    public String getSummary() {
        return "Spawn Reward " + CinematicFrame.formatTicks(this.duration);
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
        config.set(path + "." + KEY_FIREWORK, this.firework);
    }

    /**
     * @return whether a celebratory firework accompanies the reveal, reusing the same effect the
     * Simple Roll opening ends on.
     */
    public boolean isFirework() {
        return this.firework;
    }

    public void setFirework(boolean firework) {
        this.firework = firework;
    }
}
