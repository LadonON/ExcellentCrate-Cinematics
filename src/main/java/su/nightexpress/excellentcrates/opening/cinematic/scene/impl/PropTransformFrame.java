package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.Easing;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;

/**
 * Moves, spins and scales the crate prop.
 *
 * <p>This replaces "play model animation": the plugin has no ModelEngine hook, so a prop is
 * animated by keyframing its transform. Offsets are relative to the scene's prop location, which
 * means a scene keeps working after the whole set is moved somewhere else in the world.
 *
 * <p>Applies to whichever prop backs the scene — Nexo furniture or the vanilla display-entity
 * fallback — because both are driven through the same display entity.
 */
public class PropTransformFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 20L;
    private static final double DEFAULT_SCALE  = 1.0D;

    public static final String KEY_OFFSET_X = "Offset.X";
    public static final String KEY_OFFSET_Y = "Offset.Y";
    public static final String KEY_OFFSET_Z = "Offset.Z";
    public static final String KEY_YAW      = "Rotation.Yaw";
    public static final String KEY_PITCH    = "Rotation.Pitch";
    public static final String KEY_SCALE    = "Scale";
    public static final String KEY_EASING   = "Easing";

    private double offsetX, offsetY, offsetZ;
    private float  yaw, pitch;
    private double scale;
    private Easing easing;

    public PropTransformFrame(long duration, double offsetX, double offsetY, double offsetZ,
                              float yaw, float pitch, double scale, @NotNull Easing easing) {
        super(duration);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.setScale(scale);
        this.easing = easing;
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new PropTransformFrame(DEFAULT_DURATION, 0, 0, 0, 0, 0, DEFAULT_SCALE, Easing.EASE_IN_OUT);
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        long duration = readDuration(config, path, DEFAULT_DURATION);

        double offsetX = config.getDouble(path + "." + KEY_OFFSET_X, 0D);
        double offsetY = config.getDouble(path + "." + KEY_OFFSET_Y, 0D);
        double offsetZ = config.getDouble(path + "." + KEY_OFFSET_Z, 0D);
        float yaw = (float) config.getDouble(path + "." + KEY_YAW, 0D);
        float pitch = (float) config.getDouble(path + "." + KEY_PITCH, 0D);
        double scale = config.getDouble(path + "." + KEY_SCALE, DEFAULT_SCALE);
        Easing easing = Easing.byName(config.getString(path + "." + KEY_EASING, ""));

        return new PropTransformFrame(duration, offsetX, offsetY, offsetZ, yaw, pitch, scale, easing);
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.PROP_TRANSFORM;
    }

    @Override
    @NotNull
    public String getSummary() {
        return "Prop Transform " + CinematicFrame.formatTicks(this.duration);
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
        config.set(path + "." + KEY_OFFSET_X, this.offsetX);
        config.set(path + "." + KEY_OFFSET_Y, this.offsetY);
        config.set(path + "." + KEY_OFFSET_Z, this.offsetZ);
        config.set(path + "." + KEY_YAW, this.yaw);
        config.set(path + "." + KEY_PITCH, this.pitch);
        config.set(path + "." + KEY_SCALE, this.scale);
        config.set(path + "." + KEY_EASING, this.easing.name());
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

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public double getScale() {
        return this.scale;
    }

    /**
     * A zero or negative scale would make the prop vanish with no obvious cause, so it is floored
     * at a small positive value.
     */
    public void setScale(double scale) {
        this.scale = Math.max(0.01D, scale);
    }

    @NotNull
    public Easing getEasing() {
        return this.easing;
    }

    public void setEasing(@NotNull Easing easing) {
        this.easing = easing;
    }
}
