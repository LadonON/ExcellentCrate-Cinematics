package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.util.pos.WorldPos;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;

/**
 * A cinematic scene: a stage location and which existing opening animation actually runs there.
 *
 * <p>The cinematic itself does not render anything or roll any reward. It teleports the player to
 * {@link #getStage()} and hands off to whichever opening the admin picked in {@link #getOpeningId()}
 * — {@code simple_roll}, {@code csgo}, or any other id under {@code openings/} — letting that
 * opening's own, already-built visuals and reward logic run exactly as they would if the player had
 * clicked a crate configured with that id directly. Once it finishes, the player is teleported back.
 *
 * <p>One scene corresponds to one file under {@code openings/cinematic/}, so a scene's id is its file
 * name and crates select it by that id — exactly how every other opening type is chosen.
 */
public class CinematicScene {

    public static final String KEY_NAME          = "Name";
    public static final String KEY_STAGE         = "Stage";
    public static final String KEY_OPENING_ID    = "Opening";
    public static final String KEY_CAMERA_HEIGHT = "Camera_Height";
    public static final String KEY_CRATE_BLOCK   = "Crate_Block";
    public static final String KEY_OPENING_DELAY = "Opening_Delay";
    public static final String KEY_END_DELAY     = "End_Delay";

    /** How far above the stage the locked camera sits when a scene does not say otherwise. */
    public static final double DEFAULT_CAMERA_HEIGHT = 1.7D;

    /** How many ticks after the player arrives the delegate opening starts, by default. */
    public static final int DEFAULT_OPENING_DELAY = 0;

    /** How many ticks after the delegate opening finishes the player is teleported back, by default. */
    public static final int DEFAULT_END_DELAY = 0;

    private final String id;

    private String     name;
    private WorldPoint stage;
    private String     openingId;
    private double     cameraHeight;
    private WorldPos   crateBlock;
    private int        openingDelay;
    private int        endDelay;

    public CinematicScene(@NotNull String id) {
        this.id = id.toLowerCase();
        this.name = id;
        this.stage = WorldPoint.empty();
        this.openingId = "";
        this.cameraHeight = DEFAULT_CAMERA_HEIGHT;
        this.crateBlock = WorldPos.empty();
        this.openingDelay = DEFAULT_OPENING_DELAY;
        this.endDelay = DEFAULT_END_DELAY;
    }

    @NotNull
    public static CinematicScene read(@NotNull ConfigurationSection config, @NotNull String id) {
        CinematicScene scene = new CinematicScene(id);

        scene.setName(config.getString(KEY_NAME, id));
        scene.setStage(WorldPoint.read(config, KEY_STAGE));
        scene.setOpeningId(config.getString(KEY_OPENING_ID, ""));
        scene.setCameraHeight(config.getDouble(KEY_CAMERA_HEIGHT, DEFAULT_CAMERA_HEIGHT));
        scene.setCrateBlock(WorldPos.deserialize(config.getString(KEY_CRATE_BLOCK, "")));
        scene.setOpeningDelay(config.getInt(KEY_OPENING_DELAY, DEFAULT_OPENING_DELAY));
        scene.setEndDelay(config.getInt(KEY_END_DELAY, DEFAULT_END_DELAY));

        return scene;
    }

    /**
     * Writes the scene back out. Scene files that still carry the old {@code Model},
     * {@code Model_Animation}, {@code Model_Yaw} and {@code Start_Delay} keys from when cinematics
     * could spawn a ModelEngine prop keep them on disk until the file is next saved, at which point
     * they are dropped — they are simply never read, so a leftover key changes nothing.
     */
    public void write(@NotNull ConfigurationSection config) {
        config.set(KEY_NAME, this.name);
        this.stage.write(config, KEY_STAGE);
        config.set(KEY_OPENING_ID, this.openingId);
        config.set(KEY_CAMERA_HEIGHT, this.cameraHeight);
        config.set(KEY_CRATE_BLOCK, this.crateBlock.serialize());
        config.set(KEY_OPENING_DELAY, this.openingDelay);
        config.set(KEY_END_DELAY, this.endDelay);

        // Actively clear the keys the model prop used, so a scene saved from the editor doesn't keep
        // advertising options that no longer do anything.
        config.set("Model", null);
        config.set("Model_Animation", null);
        config.set("Model_Yaw", null);
        config.set("Start_Delay", null);
    }

    /**
     * A scene needs somewhere to send the player and something to run once they arrive.
     */
    public boolean isPlayable() {
        return !this.stage.isEmpty() && this.hasOpeningId();
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    /**
     * @return where the player is teleported to while the delegate opening runs. This is "the
     * actual crate" from an admin's point of view — wherever they build the real reveal to happen,
     * decorated however they like.
     */
    @NotNull
    public WorldPoint getStage() {
        return this.stage;
    }

    public void setStage(@NotNull WorldPoint stage) {
        this.stage = stage;
    }

    /**
     * @return the id of the opening animation that actually runs at the stage — any existing
     * provider id such as {@code simple_roll} or {@code csgo}. Never another cinematic scene: that
     * would either loop back on itself or chain into another teleport, neither of which is
     * meaningful.
     */
    @NotNull
    public String getOpeningId() {
        return this.openingId;
    }

    public void setOpeningId(@NotNull String openingId) {
        this.openingId = openingId.toLowerCase();
    }

    public boolean hasOpeningId() {
        return !this.openingId.isBlank();
    }

    /**
     * @return how far above the stage the player's camera is locked while the delegate opening runs.
     */
    public double getCameraHeight() {
        return this.cameraHeight;
    }

    public void setCameraHeight(double cameraHeight) {
        this.cameraHeight = cameraHeight;
    }

    /**
     * @return the actual crate block at the stage, if the admin captured one by right-clicking it
     * with the capture tool instead of clicking air. Passed through to the delegate opening so
     * block-anchored visuals — Simple Roll's reward display, most notably — render on top of it
     * instead of floating in front of wherever the locked camera happens to face.
     */
    @NotNull
    public WorldPos getCrateBlock() {
        return this.crateBlock;
    }

    public void setCrateBlock(@NotNull WorldPos crateBlock) {
        this.crateBlock = crateBlock;
    }

    public boolean hasCrateBlock() {
        return !this.crateBlock.isEmpty();
    }

    /**
     * @return how many ticks after the player arrives at the stage the delegate opening actually
     * starts. Zero means it starts the instant the player arrives.
     */
    public int getOpeningDelay() {
        return this.openingDelay;
    }

    public void setOpeningDelay(int openingDelay) {
        this.openingDelay = Math.max(0, openingDelay);
    }

    /**
     * @return how many ticks after the delegate opening finishes the player is teleported back and
     * the model prop removed. Zero means the return happens the same tick the delegate finishes.
     */
    public int getEndDelay() {
        return this.endDelay;
    }

    public void setEndDelay(int endDelay) {
        this.endDelay = Math.max(0, endDelay);
    }
}
