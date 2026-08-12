package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;

/**
 * Emits a particle burst at the prop.
 *
 * <p>Like {@link PlaySoundFrame}, the particle is held as a key string and resolved against the
 * registry at playback, so an unknown name is skipped rather than failing the scene.
 */
public class ParticleFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 0L;

    public static final String KEY_PARTICLE = "Particle";
    public static final String KEY_COUNT    = "Count";
    public static final String KEY_OFFSET_X = "Offset.X";
    public static final String KEY_OFFSET_Y = "Offset.Y";
    public static final String KEY_OFFSET_Z = "Offset.Z";
    public static final String KEY_SPEED    = "Speed";

    private static final String DEFAULT_PARTICLE = "minecraft:cloud";
    private static final int    DEFAULT_COUNT    = 20;
    private static final double DEFAULT_OFFSET   = 0.3D;
    private static final double DEFAULT_SPEED    = 0.05D;

    private String particleKey;
    private int    count;
    private double offsetX, offsetY, offsetZ;
    private double speed;

    public ParticleFrame(long duration, @NotNull String particleKey, int count,
                         double offsetX, double offsetY, double offsetZ, double speed) {
        super(duration);
        this.particleKey = particleKey;
        this.setCount(count);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.setSpeed(speed);
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new ParticleFrame(DEFAULT_DURATION, DEFAULT_PARTICLE, DEFAULT_COUNT, DEFAULT_OFFSET, DEFAULT_OFFSET, DEFAULT_OFFSET, DEFAULT_SPEED);
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        long duration = readDuration(config, path, DEFAULT_DURATION);
        String particle = config.getString(path + "." + KEY_PARTICLE, DEFAULT_PARTICLE);
        int count = config.getInt(path + "." + KEY_COUNT, DEFAULT_COUNT);
        double offsetX = config.getDouble(path + "." + KEY_OFFSET_X, DEFAULT_OFFSET);
        double offsetY = config.getDouble(path + "." + KEY_OFFSET_Y, DEFAULT_OFFSET);
        double offsetZ = config.getDouble(path + "." + KEY_OFFSET_Z, DEFAULT_OFFSET);
        double speed = config.getDouble(path + "." + KEY_SPEED, DEFAULT_SPEED);

        return new ParticleFrame(duration, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.PARTICLE;
    }

    @Override
    @NotNull
    public String getSummary() {
        return "Particle " + this.particleKey;
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
        config.set(path + "." + KEY_PARTICLE, this.particleKey);
        config.set(path + "." + KEY_COUNT, this.count);
        config.set(path + "." + KEY_OFFSET_X, this.offsetX);
        config.set(path + "." + KEY_OFFSET_Y, this.offsetY);
        config.set(path + "." + KEY_OFFSET_Z, this.offsetZ);
        config.set(path + "." + KEY_SPEED, this.speed);
    }

    @NotNull
    public String getParticleKey() {
        return this.particleKey;
    }

    public void setParticleKey(@NotNull String particleKey) {
        this.particleKey = particleKey;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    public double getOffsetX() {
        return this.offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return this.offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double getOffsetZ() {
        return this.offsetZ;
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
    }

    public double getSpeed() {
        return this.speed;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0D, speed);
    }
}
