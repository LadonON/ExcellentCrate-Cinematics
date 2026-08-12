package su.nightexpress.excellentcrates.util.pos;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.NumberUtil;

import java.util.Objects;

/**
 * A precise position with a facing direction.
 *
 * <p>{@link WorldPos} snaps to whole block coordinates, which is right for crate blocks but useless
 * for a camera — a cinematic needs sub-block precision plus yaw/pitch. {@code WorldPoint} is the
 * counterpart used by camera keyframes, the player stand-in spot, and the prop location.
 *
 * <p>Serialized form is {@code x,y,z,yaw,pitch,world}. The world name comes last and is not split
 * on, so world names containing commas survive a round trip.
 */
public class WorldPoint {

    public static final WorldPoint EMPTY = new WorldPoint("", 0, 0, 0, 0, 0);

    private static final int SERIAL_PARTS = 6;

    private final String worldName;
    private final double x, y, z;
    private final float  yaw, pitch;

    public WorldPoint(@NotNull String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @NotNull
    public static WorldPoint empty() {
        return EMPTY.copy();
    }

    /**
     * Captures a location exactly as-is, including the direction it faces.
     */
    @NotNull
    public static WorldPoint from(@NotNull Location location) {
        World world = location.getWorld();
        String worldName = world == null ? "" : world.getName();

        return new WorldPoint(worldName, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    @NotNull
    public static WorldPoint read(@NotNull ConfigurationSection config, @NotNull String path) {
        return deserialize(config.getString(path, ""));
    }

    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        config.set(path, this.serialize());
    }

    /**
     * Parses the {@code x,y,z,yaw,pitch,world} form. Malformed input yields {@link #empty()} rather
     * than throwing, so a hand-edited config can never break scene loading.
     */
    @NotNull
    public static WorldPoint deserialize(@Nullable String str) {
        if (str == null) return empty();

        // Limit the split so a world name containing commas stays in one piece.
        String[] split = str.split(",", SERIAL_PARTS);
        if (split.length < SERIAL_PARTS) return empty();

        double x = NumberUtil.getAnyDouble(split[0], 0);
        double y = NumberUtil.getAnyDouble(split[1], 0);
        double z = NumberUtil.getAnyDouble(split[2], 0);
        float yaw = (float) NumberUtil.getAnyDouble(split[3], 0);
        float pitch = (float) NumberUtil.getAnyDouble(split[4], 0);

        return new WorldPoint(split[5], x, y, z, yaw, pitch);
    }

    @NotNull
    public String serialize() {
        return this.x + "," + this.y + "," + this.z + "," + this.yaw + "," + this.pitch + "," + this.worldName;
    }

    /**
     * @return {@code true} when no world has been captured yet, which is how the editor tells
     * "location not set" from a genuine position at the world origin.
     */
    public boolean isEmpty() {
        return this.worldName.isEmpty();
    }

    @Nullable
    public World getWorld() {
        return this.worldName.isEmpty() ? null : Bukkit.getWorld(this.worldName);
    }

    /**
     * @return a {@link Location} in the referenced world, or {@code null} if that world is not loaded.
     */
    @Nullable
    public Location toLocation() {
        World world = this.getWorld();
        if (world == null) return null;

        return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    @NotNull
    public WorldPoint copy() {
        return new WorldPoint(this.worldName, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    @NotNull
    public String getWorldName() {
        return this.worldName;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof WorldPoint other)) return false;

        return Double.compare(this.x, other.x) == 0
            && Double.compare(this.y, other.y) == 0
            && Double.compare(this.z, other.z) == 0
            && Float.compare(this.yaw, other.yaw) == 0
            && Float.compare(this.pitch, other.pitch) == 0
            && Objects.equals(this.worldName, other.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.worldName, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    @Override
    public String toString() {
        return "WorldPoint{" + this.serialize() + "}";
    }
}
