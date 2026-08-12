package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.*;
import su.nightexpress.nightcore.util.Enums;

import java.util.function.Supplier;

/**
 * The catalogue of frame kinds a cinematic timeline can contain.
 *
 * <p>Each constant bundles everything the rest of the plugin needs for that kind: the editor icon,
 * a reader that rebuilds the frame from config, and a factory producing a sensible starting frame
 * when an admin adds one through the GUI. Adding a new frame kind therefore means adding one
 * implementation class and one constant here — no switch statements elsewhere to keep in sync.
 */
public enum FrameType {

    /** Holds the timeline still, which is how gaps between other frames are expressed. */
    WAIT("Wait", Material.CLOCK, WaitFrame::read, WaitFrame::createDefault),

    /** Glides the camera to a target over time. */
    MOVE_CAMERA("Move Camera", Material.COMPASS, MoveCameraFrame::read, MoveCameraFrame::createDefault),

    /** Hard cut: the camera jumps to a target instantly. */
    TELEPORT_CAMERA("Teleport Camera", Material.ENDER_PEARL, TeleportCameraFrame::read, TeleportCameraFrame::createDefault),

    /** Moves, rotates and scales the crate prop. */
    PROP_TRANSFORM("Prop Transform", Material.ARMOR_STAND, PropTransformFrame::read, PropTransformFrame::createDefault),

    /** Plays a sound at the camera. */
    PLAY_SOUND("Play Sound", Material.NOTE_BLOCK, PlaySoundFrame::read, PlaySoundFrame::createDefault),

    /** Shows a title / subtitle to the viewer. */
    SEND_TITLE("Send Title", Material.WRITABLE_BOOK, SendTitleFrame::read, SendTitleFrame::createDefault),

    /** Reveals the rolled reward at the scene's reward offset. */
    SPAWN_REWARD("Spawn Reward", Material.CHEST, SpawnRewardFrame::read, SpawnRewardFrame::createDefault),

    /** Emits a particle burst at the prop. */
    PARTICLE("Particle", Material.BLAZE_POWDER, ParticleFrame::read, ParticleFrame::createDefault);

    private final String                  name;
    private final Material                icon;
    private final Reader                  reader;
    private final Supplier<CinematicFrame> factory;

    FrameType(@NotNull String name, @NotNull Material icon, @NotNull Reader reader, @NotNull Supplier<CinematicFrame> factory) {
        this.name = name;
        this.icon = icon;
        this.reader = reader;
        this.factory = factory;
    }

    /**
     * Rebuilds a frame of this type from config.
     */
    @NotNull
    public CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        return this.reader.read(config, path);
    }

    /**
     * @return a new frame of this type with editor-friendly defaults.
     */
    @NotNull
    public CinematicFrame create() {
        return this.factory.get();
    }

    /**
     * Reads the {@code Type} discriminator at {@code path}.
     *
     * @return the matching type, or {@code null} if the key is missing or names an unknown type.
     * Callers skip unknown frames rather than failing the whole scene, so a config written by a
     * newer version still loads on an older one.
     */
    @Nullable
    public static FrameType readType(@NotNull ConfigurationSection config, @NotNull String path) {
        String raw = config.getString(path + "." + AbstractFrame.KEY_TYPE);
        return raw == null ? null : Enums.parse(raw, FrameType.class).orElse(null);
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public Material getIcon() {
        return this.icon;
    }

    /**
     * Rebuilds a frame from a config section. Declared as a named type rather than a
     * {@code BiFunction} so the method references above read clearly.
     */
    @FunctionalInterface
    public interface Reader {

        @NotNull
        CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path);
    }
}
