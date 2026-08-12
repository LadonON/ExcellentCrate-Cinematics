package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;

/**
 * Plays a sound for the viewer.
 *
 * <p>The sound is stored as a key string rather than a {@code Sound} constant. Sounds are a
 * registry in modern Minecraft, so a key also accepts resource-pack sounds a server has added, and
 * an unrecognised key degrades to silence instead of breaking scene loading.
 */
public class PlaySoundFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 0L;

    public static final String KEY_SOUND  = "Sound";
    public static final String KEY_VOLUME = "Volume";
    public static final String KEY_PITCH  = "Pitch";

    private static final String DEFAULT_SOUND  = "minecraft:block.note_block.bell";
    private static final float  DEFAULT_VOLUME = 1.0F;
    private static final float  DEFAULT_PITCH  = 1.0F;

    private String soundKey;
    private float  volume;
    private float  pitch;

    public PlaySoundFrame(long duration, @NotNull String soundKey, float volume, float pitch) {
        super(duration);
        this.soundKey = soundKey;
        this.setVolume(volume);
        this.setPitch(pitch);
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new PlaySoundFrame(DEFAULT_DURATION, DEFAULT_SOUND, DEFAULT_VOLUME, DEFAULT_PITCH);
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        long duration = readDuration(config, path, DEFAULT_DURATION);
        String sound = config.getString(path + "." + KEY_SOUND, DEFAULT_SOUND);
        float volume = (float) config.getDouble(path + "." + KEY_VOLUME, DEFAULT_VOLUME);
        float pitch = (float) config.getDouble(path + "." + KEY_PITCH, DEFAULT_PITCH);

        return new PlaySoundFrame(duration, sound, volume, pitch);
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.PLAY_SOUND;
    }

    @Override
    @NotNull
    public String getSummary() {
        return "Sound " + this.soundKey;
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
        config.set(path + "." + KEY_SOUND, this.soundKey);
        config.set(path + "." + KEY_VOLUME, this.volume);
        config.set(path + "." + KEY_PITCH, this.pitch);
    }

    @NotNull
    public String getSoundKey() {
        return this.soundKey;
    }

    public void setSoundKey(@NotNull String soundKey) {
        this.soundKey = soundKey;
    }

    public float getVolume() {
        return this.volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0F, volume);
    }

    public float getPitch() {
        return this.pitch;
    }

    /**
     * Vanilla clamps playback pitch to {@code [0.5, 2.0]}; values outside that range silently do
     * nothing, so they are clamped here where the admin can see the stored result.
     */
    public void setPitch(float pitch) {
        this.pitch = (float) Math.clamp(pitch, 0.5D, 2.0D);
    }
}
