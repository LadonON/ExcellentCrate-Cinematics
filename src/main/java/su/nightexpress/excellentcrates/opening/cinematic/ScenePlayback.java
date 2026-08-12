package su.nightexpress.excellentcrates.opening.cinematic;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.api.crate.Reward;
import su.nightexpress.excellentcrates.hooks.impl.NexoHook;
import su.nightexpress.excellentcrates.opening.OpeningUtils;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.*;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.Players;

import java.util.List;

/**
 * Runs a {@link CinematicScene} for one player: parks their body, flies the camera along the
 * timeline, drives the prop, and reveals the reward.
 *
 * <p>Kept separate from {@link CinematicOpening} so the editor's Preview/Test button can replay a
 * scene without going through the crate-opening lifecycle — no key spent, no reward rolled, no
 * cooldown touched — while still exercising exactly the same playback code an admin is trying to
 * iterate on.
 *
 * <p><b>Camera.</b> The dependency-free approach from the design brief: the player is put into
 * spectator mode and attached to an invisible marker entity that is teleported each tick. This
 * needs no packet library. If PacketEvents or ProtocolLib is present it could later be upgraded to
 * packet-driven movement transparently, exactly as the hologram handlers already degrade.
 */
public class ScenePlayback {

    /** Client-side interpolation window, in ticks. One tick matches our update rate. */
    private static final int INTERPOLATION_TICKS = 1;

    private final CratesPlugin         plugin;
    private final Player               player;
    private final CinematicScene       scene;
    private final List<CinematicFrame> frames;
    private final ItemStack            propItem;
    private final Reward               reward;

    /** Cursor into {@link #frames}. Reaching the end finishes the playback. */
    private int  frameIndex;
    /** Ticks elapsed inside the current frame. */
    private long frameTick;

    private boolean running;
    private boolean finished;

    // --- Camera ---
    private Location   cameraLocation;
    /** Camera position when the active MoveCameraFrame began, i.e. the interpolation origin. */
    private Location   cameraStart;
    private ArmorStand cameraMarker;

    // --- Prop ---
    private Location propBase;
    private Display  propDisplay;
    /** Set when the prop is a Nexo *block* rather than furniture, so it can be cleaned up. */
    private Location propBlock;

    private PropTransform propCurrent;
    private PropTransform propStart;

    // --- Reward reveal ---
    private Item rewardDisplay;

    // --- Player state to restore ---
    private Location returnLocation;
    private GameMode returnGameMode;

    /**
     * @param propItem item shown by the vanilla display-entity fallback, normally the crate's item.
     * @param reward   the already-rolled reward to reveal, or {@code null} in editor preview mode
     *                 (a {@code SpawnRewardFrame} then plays its effects with no item).
     */
    public ScenePlayback(@NotNull CratesPlugin plugin,
                         @NotNull Player player,
                         @NotNull CinematicScene scene,
                         @NotNull ItemStack propItem,
                         @Nullable Reward reward) {
        this.plugin = plugin;
        this.player = player;
        this.scene = scene;
        this.frames = List.copyOf(scene.getFrames());
        this.propItem = propItem;
        this.reward = reward;
        this.propCurrent = PropTransform.identity();
        this.propStart = PropTransform.identity();
    }

    /**
     * Parks the player and spawns the camera and prop.
     *
     * @return {@code false} if the scene cannot run — no frames, or its world is not loaded. The
     * caller is expected to finish the opening normally in that case so a misconfigured scene never
     * costs a player their key.
     */
    public boolean start() {
        if (this.running || this.frames.isEmpty()) return false;

        Location playerLocation = this.scene.getPlayerPoint().toLocation();
        if (playerLocation == null) return false;

        this.returnLocation = this.player.getLocation().clone();
        this.returnGameMode = this.player.getGameMode();

        // The camera opens on the viewing spot; the first frame moves it from there.
        this.cameraLocation = playerLocation.clone();
        this.cameraStart = playerLocation.clone();

        WorldPoint propPoint = this.scene.getPropPoint();
        this.propBase = propPoint.isEmpty() ? playerLocation.clone() : propPoint.toLocation();
        if (this.propBase == null) this.propBase = playerLocation.clone();

        this.player.teleport(playerLocation);
        this.player.setGameMode(GameMode.SPECTATOR);

        this.spawnCameraMarker(playerLocation);
        this.spawnProp();
        this.applyPropTransform(this.propCurrent);

        this.running = true;
        return true;
    }

    /**
     * Advances the timeline by one tick.
     *
     * <p>Zero-duration frames (a sound, a title, a hard cut) are consumed in a loop so several can
     * fire in the same tick; the loop stops as soon as a frame with a duration takes over.
     */
    public void tick() {
        if (!this.running || this.finished) return;

        while (this.frameIndex < this.frames.size()) {
            CinematicFrame frame = this.frames.get(this.frameIndex);

            if (this.frameTick == 0L) {
                this.onFrameStart(frame);
            }

            long duration = frame.getDuration();
            if (duration <= 0L) {
                this.advanceFrame();
                continue;
            }

            this.frameTick++;
            this.onFrameProgress(frame, (double) this.frameTick / duration);

            if (this.frameTick >= duration) {
                this.advanceFrame();
            }
            break;
        }

        if (this.frameIndex >= this.frames.size()) {
            this.finished = true;
        }

        this.applyCamera();
    }

    private void advanceFrame() {
        this.frameIndex++;
        this.frameTick = 0L;
    }

    /**
     * Restores the player and removes everything the playback spawned. Safe to call twice.
     */
    public void stop() {
        if (!this.running) return;
        this.running = false;

        if (this.cameraMarker != null) {
            // Detach before the marker dies, otherwise the client can stay stuck on a dead entity.
            if (this.player.getSpectatorTarget() == this.cameraMarker) {
                this.player.setSpectatorTarget(null);
            }
            this.cameraMarker.remove();
            this.cameraMarker = null;
        }

        if (this.propDisplay != null) {
            this.propDisplay.remove();
            this.propDisplay = null;
        }
        if (this.propBlock != null) {
            NexoHook.remove(this.propBlock);
            this.propBlock = null;
        }
        if (this.rewardDisplay != null) {
            this.rewardDisplay.remove();
            this.rewardDisplay = null;
        }

        if (this.returnGameMode != null) {
            this.player.setGameMode(this.returnGameMode);
        }
        if (this.returnLocation != null) {
            this.player.teleport(this.returnLocation);
        }
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isRunning() {
        return this.running;
    }

    // ------------------------------------------------------------------
    // Frame dispatch
    // ------------------------------------------------------------------

    /**
     * Fires a frame's instant effects and captures the interpolation origin for timed frames.
     */
    private void onFrameStart(@NotNull CinematicFrame frame) {
        switch (frame) {
            case WaitFrame ignored -> {
                // Nothing to do: the camera and prop simply hold their current state.
            }
            case MoveCameraFrame move -> this.cameraStart = this.cameraLocation.clone();
            case TeleportCameraFrame teleport -> {
                Location target = teleport.getTarget().toLocation();
                if (target != null) {
                    this.cameraLocation = target;
                    this.cameraStart = target.clone();
                }
            }
            case PropTransformFrame ignored -> this.propStart = this.propCurrent;
            case PlaySoundFrame sound -> this.playSound(sound);
            case SendTitleFrame title -> Players.sendTitle(this.player, title.getTitle(), title.getSubtitle(),
                title.getFadeIn(), title.getStay(), title.getFadeOut());
            case SpawnRewardFrame spawn -> this.spawnReward(spawn);
            case ParticleFrame particle -> this.playParticle(particle);
            default -> this.plugin.warn("Unhandled cinematic frame type: " + frame.getType());
        }

        // A zero-duration transform still has to land on its target values.
        if (frame instanceof PropTransformFrame transform && transform.getDuration() <= 0L) {
            this.applyPropTransform(PropTransform.of(transform));
        }
    }

    /**
     * Applies the interpolated state of a timed frame.
     *
     * @param progress linear progress through the frame in {@code (0, 1]}.
     */
    private void onFrameProgress(@NotNull CinematicFrame frame, double progress) {
        if (frame instanceof MoveCameraFrame move) {
            Location target = move.getTarget().toLocation();
            if (target != null) {
                this.cameraLocation = interpolate(this.cameraStart, target, move.getEasing().apply(progress));
            }
        }
        else if (frame instanceof PropTransformFrame transform) {
            double eased = transform.getEasing().apply(progress);
            this.applyPropTransform(PropTransform.lerp(this.propStart, PropTransform.of(transform), eased));
        }
    }

    /**
     * Blends two locations, taking the shortest way around for yaw so a camera panning past due
     * north does not whip the long way round.
     */
    @NotNull
    private static Location interpolate(@NotNull Location from, @NotNull Location to, double factor) {
        double x = from.getX() + (to.getX() - from.getX()) * factor;
        double y = from.getY() + (to.getY() - from.getY()) * factor;
        double z = from.getZ() + (to.getZ() - from.getZ()) * factor;

        float yaw = from.getYaw() + (float) (shortestAngle(from.getYaw(), to.getYaw()) * factor);
        float pitch = from.getPitch() + (float) ((to.getPitch() - from.getPitch()) * factor);

        return new Location(to.getWorld(), x, y, z, yaw, pitch);
    }

    /**
     * @return the signed difference between two yaw angles, normalised into {@code [-180, 180]}.
     */
    private static double shortestAngle(float from, float to) {
        double delta = (to - from) % 360.0D;
        if (delta > 180.0D) delta -= 360.0D;
        if (delta < -180.0D) delta += 360.0D;
        return delta;
    }

    // ------------------------------------------------------------------
    // Camera
    // ------------------------------------------------------------------

    private void spawnCameraMarker(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return;

        this.cameraMarker = world.spawn(location, ArmorStand.class, stand -> {
            stand.setMarker(true);        // No hitbox, so it cannot be interacted with or collided.
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setPersistent(false);   // Never written to the region file.
            stand.setCollidable(false);
        });

        this.player.setSpectatorTarget(this.cameraMarker);
    }

    private void applyCamera() {
        if (this.cameraMarker == null || this.cameraLocation == null) return;
        if (!this.cameraMarker.isValid()) return;

        this.cameraMarker.teleport(this.cameraLocation);
    }

    // ------------------------------------------------------------------
    // Prop
    // ------------------------------------------------------------------

    /**
     * Spawns the crate prop, preferring the configured Nexo model and falling back to a vanilla
     * {@link ItemDisplay} showing the crate item.
     */
    private void spawnProp() {
        if (this.propBase == null) return;

        String model = this.scene.getPropModel();
        if (this.scene.hasPropModel() && NexoHook.isInstalled()) {
            if (NexoHook.isFurniture(model)) {
                this.propDisplay = NexoHook.spawnFurniture(model, this.propBase);
                if (this.propDisplay != null) return;
            }
            else if (NexoHook.placeBlock(model, this.propBase)) {
                // A placed block has no display entity, so PropTransformFrame cannot move it.
                this.propBlock = this.propBase.clone();
                return;
            }
        }

        this.spawnFallbackProp();
    }

    /**
     * The vanilla path: an {@link ItemDisplay} showing the crate item, using native client-side
     * interpolation so it moves smoothly between our per-tick updates.
     */
    private void spawnFallbackProp() {
        World world = this.propBase.getWorld();
        if (world == null) return;

        this.propDisplay = world.spawn(this.propBase, ItemDisplay.class, display -> {
            display.setItemStack(this.propItem);
            display.setPersistent(false);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setInterpolationDelay(0);
            display.setTeleportDuration(INTERPOLATION_TICKS);
        });
    }

    /**
     * Pushes a transform onto the prop. Position and rotation ride on the entity itself; scale
     * rides on the display transformation, which is the only one of the three the entity pose
     * cannot express.
     */
    private void applyPropTransform(@NotNull PropTransform transform) {
        this.propCurrent = transform;

        if (this.propDisplay == null || !this.propDisplay.isValid() || this.propBase == null) return;

        Location location = this.propBase.clone().add(transform.x(), transform.y(), transform.z());
        location.setYaw(this.propBase.getYaw() + transform.yaw());
        location.setPitch(this.propBase.getPitch() + transform.pitch());
        this.propDisplay.teleport(location);

        float scale = (float) transform.scale();
        this.propDisplay.setTransformation(new Transformation(
            new Vector3f(),
            new AxisAngle4f(),
            new Vector3f(scale, scale, scale),
            new AxisAngle4f()
        ));
    }

    // ------------------------------------------------------------------
    // Frame effects
    // ------------------------------------------------------------------

    private void playSound(@NotNull PlaySoundFrame frame) {
        Location at = this.cameraLocation == null ? this.player.getLocation() : this.cameraLocation;
        this.player.playSound(at, frame.getSoundKey(), SoundCategory.MASTER, frame.getVolume(), frame.getPitch());
    }

    private void playParticle(@NotNull ParticleFrame frame) {
        Location at = this.propBase == null ? this.player.getLocation() : this.propBase;
        World world = at.getWorld();
        if (world == null) return;

        NamespacedKey key = NamespacedKey.fromString(frame.getParticleKey().toLowerCase());
        Particle particle = key == null ? null : Registry.PARTICLE_TYPE.get(key);
        if (particle == null) return;

        // Particles such as DUST need an extra data object; spawning them without one throws.
        if (particle.getDataType() != Void.class) return;

        world.spawnParticle(particle, at, frame.getCount(),
            frame.getOffsetX(), frame.getOffsetY(), frame.getOffsetZ(), frame.getSpeed());
    }

    /**
     * Reveals the rolled reward at the scene's reward offset.
     *
     * <p>In preview mode there is no reward, so only the accompanying effects play — the admin still
     * sees the timing of the beat they are building.
     */
    private void spawnReward(@NotNull SpawnRewardFrame frame) {
        if (this.propBase == null) return;

        Location at = this.propBase.clone().add(this.scene.getRewardOffsetX(), this.scene.getRewardOffsetY(), this.scene.getRewardOffsetZ());
        World world = at.getWorld();
        if (world == null) return;

        if (this.reward != null && this.rewardDisplay == null) {
            this.rewardDisplay = world.spawn(at, Item.class, item -> item.setVelocity(new Vector()));
            this.rewardDisplay.setPersistent(false);
            this.rewardDisplay.setCustomNameVisible(true);
            this.rewardDisplay.setGravity(false);
            this.rewardDisplay.setPickupDelay(Integer.MAX_VALUE);
            this.rewardDisplay.setUnlimitedLifetime(true);
            this.rewardDisplay.setInvulnerable(true);
            this.rewardDisplay.setItemStack(this.reward.getPreviewItem());
            EntityUtil.setCustomName(this.rewardDisplay, this.reward.getName());
        }

        if (frame.isFirework()) {
            OpeningUtils.createFirework(at);
        }
    }

    /**
     * An immutable prop pose. Kept as a record so interpolation is a plain value operation rather
     * than mutation spread across the playback's fields.
     */
    private record PropTransform(double x, double y, double z, float yaw, float pitch, double scale) {

        static PropTransform identity() {
            return new PropTransform(0, 0, 0, 0, 0, 1.0D);
        }

        static PropTransform of(@NotNull PropTransformFrame frame) {
            return new PropTransform(frame.getOffsetX(), frame.getOffsetY(), frame.getOffsetZ(),
                frame.getYaw(), frame.getPitch(), frame.getScale());
        }

        static PropTransform lerp(@NotNull PropTransform from, @NotNull PropTransform to, double factor) {
            return new PropTransform(
                from.x + (to.x - from.x) * factor,
                from.y + (to.y - from.y) * factor,
                from.z + (to.z - from.z) * factor,
                from.yaw + (float) ((to.yaw - from.yaw) * factor),
                from.pitch + (float) ((to.pitch - from.pitch) * factor),
                from.scale + (to.scale - from.scale) * factor
            );
        }
    }
}
